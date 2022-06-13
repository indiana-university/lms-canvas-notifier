package edu.iu.uits.lms.canvasnotifier.util;

/*-
 * #%L
 * canvasnotifier
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CanvasNotifierUtils {

    public final static String USERNAME_COLUMN_NAME = "username";

    public static List<String[]> deJsonCsvContent(@NonNull String jsonString) {
        // force json to be in format List<String[]>. It wants to do List<List>
        Type customType = new TypeToken<ArrayList<String[]>>(){}.getType();

        List<String[]> csvContents = new Gson().fromJson(jsonString, customType);

        return csvContents;
    }


    /**
     * Replaces all occurances of %%variable%% in parameter body with the value specified in the parameter map
     * @param lineMappedContents - Map of variable name, value.  map.get("var1") -> value 1
     * @param body
     * @return - body %%variable%% replaced
     */
    public static String getVariableReplacedBody(@NonNull Map<String, String> lineMappedContents, @NonNull String body) {
        final String variablePadString = "%%";

        for (String key : lineMappedContents.keySet()) {
            String variableString = variablePadString + key + variablePadString;

            body = body.replaceAll(variableString, lineMappedContents.get(key));
        }

        return body;
    }

    /**
     * This returns a map of the line number in the csv for all the fields. This assumes the first line (0)
     * contains the header line. So the smallest line number requested is 1
     * @param csvContents
     * @param lineNumber
     * @return Map of requested line number indexed by key of column name defined by the header (row 0)
     * Example:
     * Line 0 - username, fruit
     * Line 1 - me, apple
     * Line 2 - you, orange
     *
     * if called with lineNumber=1:
     * lineMappedContent.get("username") -> me
     * lineMappedContent.get("fruit") -> apple
     */
    public static Map<String, String> createCsvLineDataMap(List<String[]> csvContents, int lineNumber) {
        Map<String, String> lineMappedContent = new HashMap<>();

        if (csvContents.size() > 1 &&
                (lineNumber > 0 && lineNumber <= csvContents.size()) ) {
            String[] csvHeader = csvContents.get(0);

            String[] line = csvContents.get(lineNumber);

            for (int index = 0; index < line.length; index++) {
                String headerColumnName = csvHeader[index].toLowerCase().trim();
                String lineColumnValue = line[index].trim();

                if (! headerColumnName.isEmpty() && ! lineColumnValue.isEmpty()) {
                    lineMappedContent.put(headerColumnName, lineColumnValue);
                }
            }

        }

        return lineMappedContent;
    }


}
