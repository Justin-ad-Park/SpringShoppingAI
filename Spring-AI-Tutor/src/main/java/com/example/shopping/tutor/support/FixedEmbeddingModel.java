package com.example.shopping.tutor.support;

import java.util.*;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;

/** 고정 단어군으로 만든 교육용 벡터. 실제 의미 검색 품질과 구분한다. */
public class FixedEmbeddingModel implements EmbeddingModel {
    public float[] embed(Document doc) { return vector(doc.getText()); }
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings=new ArrayList<>();
        for(int i=0;i<request.getInstructions().size();i++) embeddings.add(new Embedding(vector(request.getInstructions().get(i)),i));
        return new EmbeddingResponse(embeddings);
    }
    public int dimensions() { return 5; }
    private float[] vector(String text) {
        float[] result={0,0,0,0,0};
        String[][] groups={{"컵","텀블러","보온","따뜻","생활"},{"반품","미개봉","개봉","환불","뜯"},{"배송","무료","장바구니"},{"차","커피","음료","허브티"}};
        for(int i=0;i<groups.length;i++) for(String word:groups[i]) if(text.contains(word)) result[i]+=1;
        if(result[0]+result[1]+result[2]+result[3]==0) result[4]=1;
        return result;
    }
}
