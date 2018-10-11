/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mvjf.OpenMturk;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import java.awt.Font;

/**
 *
 * @author isaac
 */
public class Message extends JPanel {
    private JLabel title;
    private JLabel text;

    public Message(String title, String text, MessageList message) {
        // this.parent = message;
        this.title = new JLabel(title + ": ", SwingConstants.LEADING);

        //Create font.
        //Font Name : Default label font
        //Font Style : Bold
        //Font Size : Default label font size

        Font newLabelFont = new Font(this.title.getFont().getName(),
        Font.BOLD,
        this.title.getFont().getSize());

        //Set JLabel font using new created font
        this.title.setFont(newLabelFont);

        this.text = new JLabel(text, SwingConstants.LEADING);
        add(this.title);
        add(this.text);
    }
}
