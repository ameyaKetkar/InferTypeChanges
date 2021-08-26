package Utilities;

import com.google.gson.Gson;
import Utilities.comby.CapturePatterns;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

public class RWUtils {



    public static String escapeMetaCharacters(String inputString){
        final String[] metaCharacters = {"\\","^","$","{","}","[","]","(",")",".","*","+","?","|","<",">","-","&","%"};

        for (int i = 0 ; i < metaCharacters.length ; i++){
            if(inputString.contains(metaCharacters[i])){
                inputString = inputString.replace(metaCharacters[i],"\\"+metaCharacters[i]);
            }
        }
        return inputString;
    }

    public static String escapeMetaCharacters(String inputString, List<String> except){
        final String[] metaCharacters = {"\\","^","$","{","}","[","]","(",")",".","*","+","?","|","<",">","-","&","%"};

        for (int i = 0 ; i < metaCharacters.length ; i++){
            if(except.contains(metaCharacters[i]))
                continue;
            if(inputString.contains(metaCharacters[i])){
                inputString = inputString.replace(metaCharacters[i],"\\"+metaCharacters[i]);
            }
        }
        return inputString;
    }

    public static String unEscapeMetaCharacters(String inputString){
        final String[] metaCharacters = {"\\","^","$","{","}","[","]","(",")",".","*","+","?","|","<",">","-","&","%"};

        for (int i = 0 ; i < metaCharacters.length ; i++){
            if(inputString.contains(metaCharacters[i])){
                inputString = inputString.replace("\\"+metaCharacters[i],metaCharacters[i]);
            }
        }
        return inputString;
    }

    public static class FileWriterSingleton {
        public static final FileWriterSingleton inst= new FileWriterSingleton();
        private FileWriterSingleton() {
            super();
        }
        public synchronized void writeToFile(String str, Path filePath) {
            try {
                Files.write(filePath, (String.join("\n", str)+"\n").getBytes(StandardCharsets.UTF_8),
                        Files.exists(filePath) ? APPEND : CREATE_NEW);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public FileWriterSingleton getInstance() {
            return inst;
        }

    }

}
