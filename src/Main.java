import com.google.gson.Gson;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

class GsonTranslated {
    public String [] text;
    public int code;
}

class MyThread extends Thread {
    private String lang;
    private File file;

    public MyThread(File file, String lang) {
        this.lang = lang;
        this.file = file;
    }

    private String readFile() throws IOException {
        char[] chars = new char[(int) file.length()];
        FileReader reader = new FileReader(file);
        reader.read(chars);
        return String.copyValueOf(chars);
    }

    private void writeFile(String str) throws IOException {
        FileWriter writer = new FileWriter(file.getName().replace("ru", lang));
        writer.write(str);
        writer.close();
    }

    private String translate(String text) throws IOException {
        String data = String.format("lang=%s&text=%s&format=plain&key=%s", lang, URLEncoder.encode(text, "UTF-8"), Main.API_KEY);

        URL url = new URL("https://translate.yandex.net/api/v1.5/tr.json/translate");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setDoOutput(true);

        OutputStream out = urlConnection.getOutputStream();
        out.write(data.getBytes());

        urlConnection.disconnect();

        Gson gson = new Gson();
        InputStreamReader streamReader = new InputStreamReader(urlConnection.getInputStream());
        GsonTranslated gsonTranslated = gson.fromJson(streamReader, GsonTranslated.class);

        if (gsonTranslated.code != 200) {
            System.out.printf("%s. Не удалось перевести. Код: %d", file.getName(), gsonTranslated.code);
        }


        return String.join("\n", gsonTranslated.text);
    }

    @Override
    public void run() {
        try {
            String text = readFile();
            String translated = translate(text);
            writeFile(translated);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

//File, Path
public class Main {

    final public static String API_KEY = "trnsl.1.1.20191001T061435Z.b35add014e4bed6b.ee611d966c8cfafe3c32a666847754fbd1bc702f";

    public static LinkedList<File> getFiles(File folder) {
        LinkedList<File> files = new LinkedList<>();
        for (File file : folder.listFiles()) {
            if (file.isFile() && file.getName().endsWith("-ru.txt"))
                files.add(file);
        }
        return files;
    }

    public static boolean langIsAvailable(String lang) throws IOException {
        String data = String.format("key=%s&ui=ru", API_KEY);

        URL url = new URL("https://translate.yandex.net/api/v1.5/tr.json/getLangs");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setDoOutput(true);

        OutputStream out = urlConnection.getOutputStream();
        out.write(data.getBytes());

        urlConnection.disconnect();

        BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String langs = br.lines().collect(Collectors.joining("\n"));
        return langs.lastIndexOf("\"" + lang + "\"") != -1;
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        Scanner in = new Scanner(System.in);

        System.out.print("Введите путь к папке: ");
        String dirPath = in.nextLine();

        System.out.print("Введите язык: ");
        String lang = in.next();

        if (!langIsAvailable(lang)) {
            System.out.println("Язык недоступен.");
            return;
        }

        LinkedList<Thread> threads = new LinkedList<>();

        LinkedList<File> files = getFiles(new File(dirPath));
        for (File file : files) {
            threads.add(new MyThread(file, lang));
        }

        for (Thread thread : threads)
            thread.start();

        for (Thread thread : threads)
            thread.join();
    }
}
