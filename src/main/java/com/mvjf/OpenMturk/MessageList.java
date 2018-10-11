/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mvjf.OpenMturk;

import java.util.List;
import javax.swing.JPanel;

/**
 *
 * @author isaac
 */
public class MessageList extends JPanel{

    public MessageList(List<String[]> messages) {

        for (int i = 0; i < messages.size(); i++) {
            Message message = new Message(messages.get(i)[0], messages.get(i)[1], this);
            add(message);
        }
    }
}
