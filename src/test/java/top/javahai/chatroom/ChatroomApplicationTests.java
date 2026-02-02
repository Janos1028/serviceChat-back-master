package top.javahai.chatroom;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Scanner;

@SpringBootTest
class ChatroomApplicationTests {
    @Test
    void generateJasyptPasswordLoop() {
        // 1. 初始化加密器配置
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();

        // 【重要】设置你的加密密钥 (Salt)
        config.setPassword("my_secret_key");
        config.setAlgorithm("PBEWithMD5AndDES");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);

        // 2. 开启无限循环模式
        Scanner scanner = new Scanner(System.in);
        System.out.println("==============================================");
        System.out.println("   Jasypt 加密小工具已启动");
        System.out.println("   输入明文并回车即可加密，输入 'exit' 退出");
        System.out.println("==============================================");

        while (true) {
            System.out.print("\n请输入要加密的密码 > ");

            // 防止某些环境没有控制台输入导致死循环
            if (!scanner.hasNextLine()) {
                break;
            }

            String plainText = scanner.nextLine().trim();

            // 3. 退出判断
            if ("exit".equalsIgnoreCase(plainText) || "quit".equalsIgnoreCase(plainText)) {
                System.out.println("程序已退出。");
                break;
            }

            if (plainText.isEmpty()) {
                continue;
            }

            // 4. 执行加密并输出
            try {
                String encryptedText = encryptor.encrypt(plainText);
                System.out.println("加密结果: ENC(" + encryptedText + ")");
            } catch (Exception e) {
                System.err.println("加密失败: " + e.getMessage());
            }
        }
    }
}
