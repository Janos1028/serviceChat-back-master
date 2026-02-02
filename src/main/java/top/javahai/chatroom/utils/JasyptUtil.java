package top.javahai.chatroom.utils;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import java.util.Scanner;

/**
 * Jasypt 加密专用工具类
 * 这是一个普通的 Java 程序，不是单元测试，直接运行 main 方法即可输入。
 */
public class JasyptUtil {

    public static void main(String[] args) {
        // 1. 初始化加密器配置
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();

        // ============================================
        // 【重要】在这里设置你的加密密钥 (Salt)
        // ============================================
        config.setPassword("fsuit_servie_chat");

        config.setAlgorithm("PBEWithMD5AndDES");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);

        // 2. 开启循环交互
        Scanner scanner = new Scanner(System.in);
        System.out.println("==============================================");
        System.out.println("   Jasypt 加密小工具 (交互模式)");
        System.out.println("   请输入明文密码并回车，输入 'exit' 退出");
        System.out.println("==============================================");

        while (true) {
            System.out.print("\n请输入要加密的密码： ");

            if (!scanner.hasNextLine()) break;
            String plainText = scanner.nextLine().trim();

            if ("exit".equalsIgnoreCase(plainText) || "quit".equalsIgnoreCase(plainText)) {
                System.out.println("程序已退出。");
                break;
            }

            if (plainText.isEmpty()) continue;

            try {
                String encryptedText = encryptor.encrypt(plainText);
                System.out.println("----------------------------------------------");
                System.out.println("加密结果: ENC(" + encryptedText + ")");
                System.out.println("----------------------------------------------");
            } catch (Exception e) {
                System.err.println("加密失败: " + e.getMessage());
            }
        }
    }
}
