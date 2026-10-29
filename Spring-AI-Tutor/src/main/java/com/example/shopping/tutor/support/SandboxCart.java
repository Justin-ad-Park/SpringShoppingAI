package com.example.shopping.tutor.support;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** 선택 변경 실습 전용. 실제 shopping-mcp나 장바구니를 변경하지 않는다. */
@Component
public class SandboxCart {
    private record Pending(String owner,String productId,Instant expires) {}
    private final Map<String,Pending> pending=new HashMap<>();
    private final Map<String,Map<String,Integer>> carts=new HashMap<>();
    private final Map<String,Instant> completed=new HashMap<>();
    private final Clock clock;
    public SandboxCart() { this(Clock.systemUTC()); }
    public SandboxCart(Clock clock) { this.clock=clock; }
    public synchronized Map<String,Object> preview(String owner,String productId) {
        var product=Shop.product(productId);
        if(!product.inStock()) throw new IllegalArgumentException("품절 상품입니다.");
        pending.values().removeIf(p -> p.expires().isBefore(clock.instant()));
        completed.values().removeIf(expiry -> !expiry.isAfter(clock.instant()));
        if(pending.size()>=1000 || carts.size()>=1000) throw new IllegalStateException("샌드박스 용량을 초과했습니다. 대화를 초기화하세요.");
        pending.values().removeIf(p -> p.owner().equals(owner));
        String token=UUID.randomUUID().toString();
        pending.put(token,new Pending(owner,productId,clock.instant().plusSeconds(120)));
        return Map.of("confirmationToken",token,"product",product,"expiresInSeconds",120,"quantity",1);
    }
    public synchronized Map<String,Integer> confirm(String owner,String token) {
        if(completed.getOrDefault(owner+":"+token,Instant.MIN).isAfter(clock.instant())) return Map.copyOf(carts.getOrDefault(owner,Map.of()));
        Pending p=pending.get(token);
        if(p==null || !p.owner().equals(owner) || !p.expires().isAfter(clock.instant())) throw new IllegalArgumentException("확인 토큰이 없거나 만료되었습니다.");
        var cart=carts.computeIfAbsent(owner,k -> new HashMap<>());
        cart.merge(p.productId(),1,Integer::sum);pending.remove(token);completed.put(owner+":"+token,p.expires());
        return Map.copyOf(cart);
    }
    public synchronized void reset(String owner) {
        carts.remove(owner);pending.values().removeIf(p -> p.owner().equals(owner));completed.keySet().removeIf(k -> k.startsWith(owner+":"));
    }
}
