/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conferencing;

import java.util.Map;

/**
 *
 * @author Administrator
 */
public class Message {
    String msg;
    Map<String,Integer> clock;
    
    Message(Map<String,Integer> clock, String msg)
    {
        this.clock = clock;
        this.msg = msg;
    }
            
}
