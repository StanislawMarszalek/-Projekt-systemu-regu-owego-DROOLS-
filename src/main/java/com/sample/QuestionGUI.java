package com.sample;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

public class QuestionGUI {

    private KieContainer kieContainer;

    public QuestionGUI(KieContainer kieContainer) {
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
                kSession.fireAllRules();
                // Pobieranie ProcesKontrolny
                Object proc = kSession.getObjects(o -> o.getClass().getSimpleName().equals("ProcesKontrolny")).stream().findFirst().get();
                while (true) {
                    

                    // sprawdŸ stop
                    boolean stop = (boolean) proc.getClass().getMethod("isStopProcessing").invoke(proc);
                    if (stop) {
                        textArea.append("Found the Answer\n");
                        break;
                    }

                    // pierwsze niezadane pytanie
                    Object nextP = kSession.getObjects(o -> o.getClass().getSimpleName().equals("Pytanie")).stream().filter(o -> {try {return !(boolean) o.getClass().getMethod("isJuzZadane").invoke(o);} catch (Exception ex) {throw new RuntimeException(ex);}}).findFirst().orElse(null);

                    String tresc = (String) nextP.getClass().getMethod("getTresc").invoke(nextP);
                    String[] odp = (String[]) nextP.getClass().getMethod("getOdpowiedz").invoke(nextP);

                    int answer = JOptionPane.showOptionDialog(frame,tresc,"Pytanie",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE,null,odp,odp[0]);

                    if (answer == JOptionPane.CLOSED_OPTION) {
                        // ustaw StopProcessing = true i zakoñcz
                        proc.getClass().getMethod("setStopProcessing", boolean.class).invoke(proc, true);
                        FactHandle ph = kSession.getFactHandle(proc);
                        kSession.update(ph, proc);
                        kSession.fireAllRules();
                        textArea.append("Dialog closed by the User.\n");
                        break;
                    }

                    String chosen = odp[answer];
                    FactHandle qHandle = kSession.getFactHandle(nextP);
                    nextP.getClass().getMethod("setWyborUzytkownika", String.class).invoke(nextP, chosen);
                    nextP.getClass().getMethod("setJuzZadane", boolean.class).invoke(nextP, true);
                    if (qHandle != null) kSession.update(qHandle, nextP);

                    kSession.fireAllRules();

                    textArea.append("Question: " + tresc + "\n");
                    textArea.append("User choice: " + chosen + "\n\n");
                }

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
            SwingUtilities.invokeLater(() -> new QuestionGUI(kieContainer));
    }
}


