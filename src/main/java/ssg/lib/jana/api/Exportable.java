/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.lib.jana.api;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 *
 * @author sesidoro
 */
public interface Exportable {

    void exportTo(Writer wr) throws IOException;

    void importFrom(Reader r) throws IOException;
}
