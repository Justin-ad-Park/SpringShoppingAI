package com.example.shopping.tutor.web;

import java.io.*;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;

public record MediaInput(String mime,String filename) {
    public static MediaInput validate(String caseId,String mime,byte[] bytes) {
        if(mime==null || bytes==null || bytes.length<12 || bytes.length>5*1024*1024) throw new IllegalArgumentException("파일 형식과 크기를 확인하세요.");
        if(caseId.equals("L14-01")) {
            if(!mime.equals("image/png") && !mime.equals("image/jpeg")) throw new IllegalArgumentException("PNG/JPEG만 지원합니다.");
            try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers=ImageIO.getImageReaders(input);
                if(!readers.hasNext()) throw new IllegalArgumentException("이미지가 아닙니다.");
                var reader=readers.next();
                try { reader.setInput(input);if((long)reader.getWidth(0)*reader.getHeight(0)>20_000_000) throw new IllegalArgumentException("이미지 해상도가 너무 큽니다."); }
                finally { reader.dispose(); }
            } catch(IOException e) { throw new IllegalArgumentException("이미지를 읽을 수 없습니다."); }
            return new MediaInput(mime,mime.equals("image/png")?"input.png":"input.jpg");
        }
        String prefix=new String(bytes,0,Math.min(12,bytes.length),StandardCharsets.ISO_8859_1);
        boolean valid=switch(mime) {
            case "audio/wav","audio/x-wav" -> prefix.startsWith("RIFF") && prefix.substring(8).startsWith("WAVE");
            case "audio/mpeg" -> prefix.startsWith("ID3") || ((bytes[0]&255)==255 && (bytes[1]&224)==224);
            case "audio/webm" -> (bytes[0]&255)==0x1a && (bytes[1]&255)==0x45 && (bytes[2]&255)==0xdf && (bytes[3]&255)==0xa3;
            case "audio/mp4" -> prefix.substring(4).startsWith("ftyp");
            default -> false;
        };
        if(!valid) throw new IllegalArgumentException("지원하는 음성 파일이 아닙니다.");
        return new MediaInput(mime,switch(mime){case "audio/wav","audio/x-wav" -> "input.wav";case "audio/webm" -> "input.webm";case "audio/mp4" -> "input.m4a";default -> "input.mp3";});
    }
}
