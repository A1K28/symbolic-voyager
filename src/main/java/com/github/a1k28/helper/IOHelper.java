package com.github.a1k28.helper;

import lombok.NoArgsConstructor;

import java.io.*;

@NoArgsConstructor
public class IOHelper {
    public static void writeFile(String fileName, String data) throws IOException {
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(data);
        }
    }

    public static String readFile(String fileName) throws IOException {
        StringBuilder sb = new StringBuilder();
        try(BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
