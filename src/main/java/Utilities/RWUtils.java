package Utilities;

import com.google.gson.Gson;
import Utilities.comby.CapturePatterns;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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


//    public static void writeAsJson(String fileNameTypechange, Map<Tuple2<String, String>, List<Update>> groupedByTemplates, String beforeTypeTemplate, String afterTypeTemplate) {
//        List<String> collect = groupedByTemplates.entrySet().stream()
//                //midnight controller .filter(x -> x.getValue().size() > 1)
//                .map(Tuple::fromEntry)
//                .map(x -> x.map2(y -> y.stream().map(Update::getAsInstance).collect(toList())))
//                .map(x -> new InferredMappings(beforeTypeTemplate, afterTypeTemplate, x._1()._1(), x._1()._2(), x._2()))
//                .map(x -> new Gson().toJson(x))
//                .collect(toList());
//        try {
//            Files.write(outputFolder.resolve(fileNameTypechange), String.join("\n", collect).getBytes(), StandardOpenOption.APPEND);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}
