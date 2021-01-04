package com.warm_ice;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Random;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class Main {
  public static void main(String[] args) {
    GUI.createGUI();
  }
}

class GUI {
  static void createGUI() {
    String[] words = new String[0];
    try { // 把单词数组在启动程序时读进内存，避免反复读取硬盘
      words =
          Tokenize.tokenize(
              FileIO.readStrFromFile(
                  new File(FileIO.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                          .getParentFile()
                          .getPath()
                      + "/train.txt"));
    } catch (NullPointerException e) { // 如果找不到train.txt或路径有问题，报错
      JOptionPane.showMessageDialog(
          null, "Can't find train.txt!", "Error", JOptionPane.ERROR_MESSAGE);
      System.exit(-1); // 退出程序，代码-1
    } catch (URISyntaxException e) {
      JOptionPane.showMessageDialog(
          null, "train.txt path error!", "Error", JOptionPane.ERROR_MESSAGE);
      System.exit(-1); // 退出程序，代码-1
    }
    Hashtable<String, ArrayList<String>> bigrams = Ngram.get2Grams(words);
    Hashtable<String, Hashtable<String, ArrayList<String>>> trigrams = Ngram.get3Grams(words);
    JFrame jFrame = new JFrame();
    jFrame.setTitle("MarkovGen v1.1");
    int[] screenSize = getScreenSize();
    jFrame.setSize((int) Math.round(screenSize[0] * 0.45), (int) Math.round(screenSize[1] * 0.45));
    JButton jButton = new JButton("Generate!");
    JRadioButton jRadioButton2 = new JRadioButton("2-gram");
    JRadioButton jRadioButton3 = new JRadioButton("3-gram");
    jRadioButton3.setSelected(true); // 默认选中“3-gram”
    JPanel jPanel = new JPanel();
    jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.X_AXIS));
    jPanel.add(jButton); // 把“Generate!”放在jPanel上
    jPanel.add(Box.createHorizontalStrut(5)); // “Generate!”和“Algorithm: ”中间添加5像素间隔
    jPanel.add(new JLabel("Algorithm: ")); // 在两个单选框左边的“Algorithm: ”
    jPanel.add(jRadioButton2); // 把“2-gram”放在jPanel（在窗口顶部）上
    jPanel.add(jRadioButton3); // 把“3-gram”放在jPanel上
    JTextArea jTextArea = new JTextArea(); // 用来放生成结果的JTextArea
    jTextArea.setLineWrap(true); // 设置自动换行
    jTextArea.setWrapStyleWord(true); // 设置只在单词边界自动换行，避免把单词切成两段
    jTextArea.setEditable(false); // 设置禁止编辑
    JScrollPane jScrollPane = new JScrollPane(jTextArea); // jTextArea的滚动条
    jScrollPane.setVerticalScrollBarPolicy(
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED); // 滚动条只在文字多到放不下时出现
    jFrame.setLayout(new BorderLayout()); // jFrame采用BorderLayout
    Container container = jFrame.getContentPane(); // jFrame的Container
    container.add(BorderLayout.NORTH, jPanel); // jFrame顶部放jPanel
    container.add(BorderLayout.CENTER, jScrollPane); // jFrame“中间”（其实是中间和下面）放带滚动条的jTextArea
    String[] finalWords = words; // words要这样转换一下才能用在lambda中
    jButton.addActionListener(
        e -> { // ”Generate!“的ActionListener
          if (jRadioButton2.isSelected()) // 两个单选框都没被选中时（应该不会出现）使用3-gram
          jTextArea.setText(Tokenize.untokenize(MarkovGen.bigramGen(finalWords, bigrams, 1000)));
          else
            jTextArea.setText(
                Tokenize.untokenize(MarkovGen.trigramGen(finalWords, bigrams, trigrams)));
        });
    jRadioButton2.addActionListener(
        e -> { // ”2-gram“的ActionListener
          jRadioButton3.setSelected(false); // 防止两个单选框同时被选中
          jRadioButton2.setSelected(true); // 防止没有单选框被选中
        });
    jRadioButton3.addActionListener(
        e -> { // ”3-gram“的ActionListener，同样的套路
          jRadioButton2.setSelected(false);
          jRadioButton3.setSelected(true);
        });
    jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // 设置关闭窗口时退出程序
    jFrame.setLocationRelativeTo(null); // 让窗口出现在屏幕中间，而不是左上角
    jFrame.setVisible(true); // 一切都准备好后再让窗口可见
  }

  private static int[] getScreenSize() {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    return new int[] {screenSize.width, screenSize.height};
  }
}

class Ngram {
  static Hashtable<String, ArrayList<String>> get2Grams(String[] words) {
    Hashtable<String, ArrayList<String>> bigrams = new Hashtable<>();
    for (int i = 0; i < words.length - 1; i++) {
      if (!bigrams.containsKey(words[i])) bigrams.put(words[i], new ArrayList<>());
      bigrams.get(words[i]).add(words[i + 1]);
    }
    return bigrams;
  }

  static Hashtable<String, Hashtable<String, ArrayList<String>>> get3Grams(String[] words) {
    Hashtable<String, Hashtable<String, ArrayList<String>>> trigrams = new Hashtable<>();
    for (int i = 0; i < words.length - 2; i++) {
      if (trigrams.containsKey(words[i])) {
        if (!trigrams.get(words[i]).containsKey(words[i + 1]))
          trigrams.get(words[i]).put(words[i + 1], new ArrayList<>());
      } else {
        trigrams.put(words[i], new Hashtable<>());
        trigrams.get(words[i]).put(words[i + 1], new ArrayList<>());
      }
      trigrams.get(words[i]).get(words[i + 1]).add(words[i + 2]);
    }
    return trigrams;
  }
}

class Tokenize {
  static String[] tokenize(String text) {
    return Pattern.compile("\\w+|[^\\w\\s]+")
        .matcher(text)
        .results()
        .map(MatchResult::group)
        .toArray(String[]::new);
  }

  static String untokenize(String[] words) {
    return String.join(" ", words)
        .replace(" .", ".")
        .replace(" ,", ",")
        .replace(" :", ":")
        .replace(" ;", ";")
        .replace(" ?", "?")
        .replace(" !", "!")
        .replace(" %", "%")
        .replace("`` ", "\"")
        .replace(" ''", "\"")
        .replace(". . .", "...")
        .replace(" ( ", " (")
        .replace(" ) ", ") ")
        .replace(" '", "'")
        .replace(" n't", "n't")
        .replace("can not", "cannot")
        .replace(" ` ", " '")
        .replace(" - ", "-")
        .strip();
  }
}

class MarkovGen {
  static String[] bigramGen(
      String[] words, Hashtable<String, ArrayList<String>> bigrams, int wordCount) {
    Random random = new Random();
    String[] firstWords =
        Arrays.stream(words)
            .filter(word -> Character.isUpperCase(word.charAt(0)))
            .toArray(String[]::new);
    String lastAppend = firstWords[random.nextInt(firstWords.length)];
    ArrayList<String> output = new ArrayList<>();
    for (int i = 0; i < wordCount; i++) {
      output.add(lastAppend);
      if (bigrams.containsKey(lastAppend)) {
        ArrayList<String> candidates = bigrams.get(lastAppend);
        lastAppend = candidates.get(random.nextInt(candidates.size()));
      } else break;
    }
    return output.toArray(String[]::new);
  }

  static String[] trigramGen(
      String[] words,
      Hashtable<String, ArrayList<String>> bigrams,
      Hashtable<String, Hashtable<String, ArrayList<String>>> trigrams) {
    Random random = new Random();
    String[] last2Appends = bigramGen(words, bigrams, 2);
    ArrayList<String> output = new ArrayList<>();
    output.add(last2Appends[0]);
    for (int i = 0; i < 1000; i++) { // 生成 1000 个单词（实际只会少不会多）
      output.add(last2Appends[1]);
      if (trigrams.containsKey(last2Appends[0])) {
        if (trigrams.containsKey(last2Appends[1])) {
          ArrayList<String> candidates = trigrams.get(last2Appends[0]).get(last2Appends[1]); // 使用泛型
          last2Appends =
              new String[] {last2Appends[1], candidates.get(random.nextInt(candidates.size()))};
        } else break;
      } else break;
    }
    return output.toArray(String[]::new);
  }
}

class FileIO {
  static String readStrFromFile(String path) {
    try {
      return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    } catch (IOException e) {
      return null;
    }
  }
}
