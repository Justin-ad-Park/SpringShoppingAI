package com.example.shopping.tutor.web;

import java.lang.reflect.*;
import java.util.*;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import com.example.shopping.tutor.support.*;

/** 시작할 때 등록된 @TutorCase 메소드만 실행한다. 외부 클래스명/메소드명을 받지 않는다. */
@Component
public class LessonCatalog {
    public record Entry(String id,String title,String sample,String goal,boolean stream,String requires,String source,String method,String test) {}
    private record Target(Object bean,Method method,Entry entry) {}
    private final SortedMap<String,Target> targets=new TreeMap<>();
    public LessonCatalog(ApplicationContext context) {
        for(String name:context.getBeanDefinitionNames()) {
            Class<?> type=context.getType(name);
            if(type==null || !type.getPackageName().equals("com.example.shopping.tutor.lesson")) continue;
            for(Method method:type.getMethods()) {
                TutorCase a=method.getAnnotation(TutorCase.class);if(a==null) continue;
                if(!Arrays.equals(method.getParameterTypes(),new Class<?>[]{RunContext.class}) || method.getReturnType()!=Outcome.class)
                    throw new IllegalStateException("잘못된 case 메소드: "+method);
                String source="src/main/java/"+type.getName().replace('.','/')+".java";
                Entry entry=new Entry(a.id(),a.title(),a.sample(),a.goal(),a.stream(),a.requires(),source,method.getName(),"src/test/java/com/example/shopping/tutor/CurriculumTest.java ("+a.id()+")");
                if(targets.put(a.id(),new Target(context.getBean(name),method,entry))!=null) throw new IllegalStateException("중복 caseId: "+a.id());
            }
        }
    }
    public List<Entry> entries() { return targets.values().stream().map(Target::entry).toList(); }
    public Entry entry(String id) { return target(id).entry(); }
    private Target target(String id) {
        var target=targets.get(id);if(target==null) throw new NoSuchElementException("없는 caseId입니다.");return target;
    }
    public Outcome execute(String id,RunContext c) {
        var target=target(id);
        try { return (Outcome)target.method().invoke(target.bean(),c); }
        catch(InvocationTargetException e) {
            if(e.getCause() instanceof RuntimeException cause) throw cause;
            throw new IllegalStateException("실습 실행 실패",e.getCause());
        } catch(ReflectiveOperationException e) { throw new IllegalStateException("실습 연결 실패",e); }
    }
}
