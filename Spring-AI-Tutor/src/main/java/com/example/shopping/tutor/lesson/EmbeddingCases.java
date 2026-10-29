package com.example.shopping.tutor.lesson;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.example.shopping.tutor.support.*;

@Component
public class EmbeddingCases {
    @TutorCase(id="L07-01",title="임베딩과 cosine 유사도",sample="보온용 컵",goal="벡터 숫자와 유사도를 확인합니다. STUB 벡터는 교육용 단어군입니다.")
    public Outcome compareSimilarity(RunContext c) {
        float[] query=c.embedding.embed(c.message);
        float[] cup=c.embedding.embed("따뜻하게 마시는 텀블러");
        float[] shipping=c.embedding.embed("배송 문의");
        return Outcome.ok("유사도는 확률이 아닙니다.",Map.of("dimensions",query.length,"cupSimilarity",cosine(query,cup),"shippingSimilarity",cosine(query,shipping)));
    }
    public static double cosine(float[] a,float[] b) {
        if(a.length!=b.length) throw new IllegalArgumentException("벡터 차원이 다릅니다. 재색인이 필요합니다.");
        double dot=0,aa=0,bb=0;
        for(int i=0;i<a.length;i++) { dot+=a[i]*b[i];aa+=a[i]*a[i];bb+=b[i]*b[i]; }
        return aa==0 || bb==0?0:dot/Math.sqrt(aa*bb);
    }
}
