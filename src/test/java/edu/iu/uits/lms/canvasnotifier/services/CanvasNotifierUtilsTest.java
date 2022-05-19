package edu.iu.uits.lms.canvasnotifier.services;

import edu.iu.uits.lms.canvasnotifier.util.CanvasNotifierUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class CanvasNotifierUtilsTest {

    private List<String[]> csvContents;

    private final String[] recipients = new String[]{"senduser1", "senduser2", "senduser3", "senduser4"};
    private String jsonCsvString;


    @BeforeEach
    public void setup() {
        csvContents = new ArrayList<>();
        csvContents.add(new String[]{"Username", "variableName1", "variableName2"});
        csvContents.add(new String[]{"Username1", "Value1-1", "Value2-1"});
        csvContents.add(new String[]{"Username2", "Value1-2", "Value2-2"});
        csvContents.add(new String[]{"Username3", "Value1-3", "Value2-3"});

        jsonCsvString = "[[\"username\",\" team\",\" superhero\"]," +
                "[\"" + recipients[0] + "\",\" Fuel\",\" spiderman\"]," +
                "[\"" + recipients[1] + "\",\" Bills\",\" Wonder Woman\"]," +
                "[\"" + recipients[2] + "\",\" Express\",\" batman\"]," +
                "[\"" + recipients[3] + "\",\" crap team\",\" me\"],[\"\"]]";
    }

    @Test
    public void testdeJsonCsvContent() {
        List<String[]> csvContent = CanvasNotifierUtils.deJsonCsvContent(jsonCsvString);

        Assertions.assertNotEquals(0, csvContent.size());
        Assertions.assertEquals(3, csvContent.get(0).length);

        Assertions.assertEquals("username", csvContent.get(0)[0].trim());
        Assertions.assertEquals("team", csvContent.get(0)[1].trim());
        Assertions.assertEquals("superhero", csvContent.get(0)[2].trim());

        Assertions.assertEquals(recipients[0], csvContent.get(1)[0].trim());
        Assertions.assertEquals("Fuel", csvContent.get(1)[1].trim());
        Assertions.assertEquals("spiderman", csvContent.get(1)[2].trim());

        Assertions.assertEquals(recipients[1], csvContent.get(2)[0].trim());
        Assertions.assertEquals("Bills", csvContent.get(2)[1].trim());
        Assertions.assertEquals("Wonder Woman", csvContent.get(2)[2].trim());

        Assertions.assertEquals(recipients[2], csvContent.get(3)[0].trim());
        Assertions.assertEquals("Express", csvContent.get(3)[1].trim());
        Assertions.assertEquals("batman", csvContent.get(3)[2].trim());

        Assertions.assertEquals(recipients[3], csvContent.get(4)[0].trim());
        Assertions.assertEquals("crap team", csvContent.get(4)[1].trim());
        Assertions.assertEquals("me", csvContent.get(4)[2].trim());

    }

    @Test
    public void testCreateCsvLineDataMap() {
        Map<String, String> lineMappedResults = CanvasNotifierUtils.createCsvLineDataMap(csvContents, 1);

        Assertions.assertTrue(lineMappedResults.containsKey(CanvasNotifierUtils.USERNAME_COLUMN_NAME));

        Assertions.assertTrue(lineMappedResults.containsKey("username"));
        Assertions.assertTrue(lineMappedResults.containsKey("variablename1"));
        Assertions.assertTrue(lineMappedResults.containsKey("variablename2"));

        // should be all lowercased
        Assertions.assertFalse(lineMappedResults.containsKey("Username"));

        Assertions.assertEquals("Username1", lineMappedResults.get("username"));
        Assertions.assertEquals("Value1-1", lineMappedResults.get("variablename1"));
        Assertions.assertEquals("Value2-1", lineMappedResults.get("variablename2"));


        // try a different line
        lineMappedResults = CanvasNotifierUtils.createCsvLineDataMap(csvContents, 2);

        Assertions.assertEquals("Username2", lineMappedResults.get("username"));
        Assertions.assertEquals("Value1-2", lineMappedResults.get("variablename1"));
        Assertions.assertEquals("Value2-2", lineMappedResults.get("variablename2"));
    }

    @Test
    public void testGetVariableReplacedBody() {
        String text = "%%username%%, how is %%variablename1%% and %%variablename2%% ?";

        Map<String, String> lineMappedResults = CanvasNotifierUtils.createCsvLineDataMap(csvContents, 1);

        Assertions.assertEquals("Username1, how is Value1-1 and Value2-1 ?",
                CanvasNotifierUtils.getVariableReplacedBody(lineMappedResults, text));
    }
}