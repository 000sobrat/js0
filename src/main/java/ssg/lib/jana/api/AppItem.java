/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.lib.jana.api;

import java.io.Serializable;

/**
 *
 * @author sesidoro
 */
public interface AppItem extends Serializable, Cloneable {
    String getId();
    void setId(String id);
}
