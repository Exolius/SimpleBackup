package com.exolius.simplebackup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class PluginUtils {

    public static String loadMessage() {
        File ms = new File(SimpleBackup.pluginLocation + "custommessage.txt");
        if(!ms.exists()) {

        }

        try {
                FileInputStream in = new FileInputStream(SimpleBackup.pluginLocation + "custommessage.txt");
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String strLine;
                for (int j = 0; j < 5; j++) {
                	SimpleBackup.message = SimpleBackup.message + br.readLine();
                }
                in.close();

            } catch (Exception exception) {
            }
        System.out.println(SimpleBackup.message);
        return SimpleBackup.message;
    }

}
