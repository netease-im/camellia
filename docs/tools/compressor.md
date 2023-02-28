
# CamelliaCompressor

## 简介
* 使用LZ4压缩算法
* 支持设置压缩阈值
* 向下兼容（解压时会检查是否压缩过，通过内部添加特定前缀判断）

## maven
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-tools</artifactId>
    <version>1.2.2</version>
</dependency>
```

## 示例
```java
public class CompressSamples {

    public static void main(String[] args) {
        CamelliaCompressor compressor = new CamelliaCompressor();

        StringBuilder data = new StringBuilder();
        for (int i=0; i<2048; i++) {
            data.append("abc");
        }
        //原始数据
        System.out.println(data.length());
        //压缩
        byte[] compress = compressor.compress(data.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println(Base64.getEncoder().encodeToString(compress));
        //解压
        byte[] decompress = compressor.decompress(compress);
        System.out.println(new String(decompress).length());
        //判断是否一致
        System.out.println(new String(decompress).equals(data.toString()));

        //直接解压原始数据，CamelliaCompressor会发现没有做过压缩，因此会直接返回
        byte[] decompress1 = compressor.decompress(data.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println(new String(decompress1).length());
        System.out.println(new String(decompress1).equals(data.toString()));
    }
}

```