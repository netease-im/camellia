
## 介绍
提供了一些工具类，包括：压缩工具类CamelliaCompressor、加解密工具类CamelliaEncryptor等  

## maven依赖
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-tools</artifactId>
    <version>a.b.c</version>
</dependency>
```

## 示例
### CamelliaCompressor示例
CamelliaCompressor可以设置压缩阈值，此外解压缩之前会判断是否压缩过，从而在某些业务场景下可以做到向下兼容  
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

### CamelliaEncryptor示例  
CamelliaEncryptor是线程安全的，支持AES加密，可以选择不同的模式（默认是AES/CBC/PKCS5Padding）；此外解密之前会判断是否加密过，从而在某些业务场景下可以做到向下兼容  
```java
public class EncryptSamples {

    public static void main(String[] args) {

        for (CamelliaEncryptAesConfig.Type type : CamelliaEncryptAesConfig.Type.values()) {
            System.out.println(">>>>>START>>>>" + type.getDesc());
            CamelliaEncryptor encryptor = new CamelliaEncryptor(new CamelliaEncryptAesConfig(type, "111"));//设置秘钥seed
            String data = "Hello Camellia";

            //原始数据
            System.out.println(data);
            //加密
            byte[] encrypt = encryptor.encrypt(data.getBytes(StandardCharsets.UTF_8));
            System.out.println(Base64.getEncoder().encodeToString(encrypt));
            //解密
            byte[] decrypt = encryptor.decrypt(encrypt);
            System.out.println(new String(decrypt));
            //判断解密后是否和原始数据一致
            System.out.println(new String(decrypt).equals(data));

            //直接对原始数据解密，CamelliaEncryptor会发现没有加密过，直接返回
            byte[] decrypt1 = encryptor.decrypt(data.getBytes(StandardCharsets.UTF_8));
            System.out.println(new String(decrypt1));
            System.out.println(new String(decrypt1).equals(data));

            System.out.println(">>>>>END>>>>" + type.getDesc());
        }
    }
}

```

### 更多示例和源码
[示例源码](/camellia-samples/camellia-tools-samples)
