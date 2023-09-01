
# CamelliaEncryptor

## 简介
* 支持AES系列加密算法，可以选择不同的模式（默认是AES/CBC/PKCS5Padding）
* 支持设置压缩密钥（sed）
* 线程安全
* 向下兼容（解压时会检查是否加密过，通过内部添加特定前缀判断）

## maven
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-tools</artifactId>
    <version>1.2.15</version>
</dependency>
```

## 示例
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