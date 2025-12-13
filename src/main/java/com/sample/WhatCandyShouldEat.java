package com.sample;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.List;


public class WhatCandyShouldEat {

    private KieContainer kieContainer;

    public WhatCandyShouldEat(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
        createAndShowGUI();
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("WhatCandyShouldIEat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 350);
        frame.setLayout(new BorderLayout());

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        JButton showQuestionsBtn = new JButton("Let's find the candy");
        frame.add(showQuestionsBtn, BorderLayout.SOUTH);

        showQuestionsBtn.addActionListener((ActionEvent e) -> {
            showQuestionsBtn.setVisible(false);
            KieSession kSession = kieContainer.newKieSession();
            try {
                Map<String, List<String>> candyMap = new HashMap<>();
                ObjectMapper mapper = new ObjectMapper();
                File jsonFile = new File("src/main/resources/candy.json");
                candyMap = mapper.readValue(jsonFile, new TypeReference<Map<String, List<String>>>() {});
                kSession.fireAllRules();
                // Getting controll process
                Object proc = kSession.getObjects(o -> o.getClass().getSimpleName().equals("ControlProcess")).stream().findFirst().get();
                
                String chosen=null;
                String shortenedQuestion=null;

                while (true) {
                                        
                    boolean stop = (boolean) proc.getClass().getMethod("isStopProcessing").invoke(proc);
                    if (stop) {
                        textArea.append("Found the Answer\n");
                        break;
                    }

                    // Looking for first ungiven question
                    Object nextP = kSession.getObjects(o -> o.getClass().getSimpleName().equals("Question")).stream().filter(o -> {
                    	try {String choice = (String) o.getClass().getMethod("getUserChoice").invoke(o);
                                    return  "NULL".equals(choice);} catch (Exception ex) {throw new RuntimeException(ex);}}).findFirst().orElse(null);
                    
                    String content = (String) nextP.getClass().getMethod("getContent").invoke(nextP);
                    String[] possibleAnswers = (String[]) nextP.getClass().getMethod("getPossibleAnswers").invoke(nextP);
                    shortenedQuestion=(String)nextP.getClass().getMethod("getShortenedQuestion").invoke(nextP);
                    int answer = JOptionPane.showOptionDialog(frame,content,"Question",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE,null,possibleAnswers,possibleAnswers[0]);


                    if (answer == JOptionPane.CLOSED_OPTION) {
                    	
                        proc.getClass().getMethod("setStopProcessing", boolean.class).invoke(proc, true);
                        FactHandle ph = kSession.getFactHandle(proc);
                        kSession.update(ph, proc);
                        kSession.fireAllRules();
                        textArea.append("Dialog closed by the User.\n");
                        break;
                    }

                    chosen = possibleAnswers[answer];
                    FactHandle qHandle = kSession.getFactHandle(nextP);
                    nextP.getClass().getMethod("setUserChoice", String.class).invoke(nextP, chosen);
                   
                    if (qHandle != null) kSession.update(qHandle, nextP);
                    
                    kSession.fireAllRules();
                    
                    textArea.append("Question: " + content + "\n");
                    textArea.append("User choice: " + chosen + "\n\n");
                }
                //Creating key to find images
                String finalKey = shortenedQuestion + "_" + chosen; 
                List<String> finalResults = candyMap.get(finalKey);
                //Showing Images
                JFrame candiesFrame = new JFrame("Candies you should eat");
                JPanel candiesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

                for (String candyName : finalResults) {
                    java.net.URL candyURL = getClass().getClassLoader().getResource("CandyPictures/" + candyName);
                   	ImageIcon candyImage = new ImageIcon(candyURL);
                   	Image sclaedImage = candyImage.getImage().getScaledInstance(300, 300, Image.SCALE_SMOOTH);
                   	candiesPanel.add(new JLabel(new ImageIcon(sclaedImage)));   
                }
                
                candiesFrame.getContentPane().add(new JScrollPane(candiesPanel));
                candiesFrame.setSize(400, 400);
                candiesFrame.setLocationRelativeTo(null);
                candiesFrame.setVisible(true);
         
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                kSession.dispose();
            }
        });

        frame.setVisible(true);
    }

    public static void main(String[] args) {
            KieContainer kieContainer = org.kie.api.KieServices.Factory.get().getKieClasspathContainer();
            SwingUtilities.invokeLater(() -> new WhatCandyShouldEat(kieContainer));
    }
}