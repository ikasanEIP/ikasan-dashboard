package org.ikasan.dashboard.ui.scheduler.util;

import org.junit.Assert;
import org.junit.Test;

public class ControlCharacterUtilsTest {

    @Test
    public void test_format_with_mixed_control_characters() {
        Assert.assertEquals("this\r\nis a string\r\nwith mixed control characters\r\n"
            , ControlCharacterUtils.format("this\ris a string\r\nwith mixed control characters\n"
            , ControlCharacterUtils.ControlCharacter.WINDOWS));

        Assert.assertEquals("this\nis a string\nwith mixed control characters\n"
            , ControlCharacterUtils.format("this\ris a string\r\nwith mixed control characters\n"
                , ControlCharacterUtils.ControlCharacter.UNIX));
    }

    @Test
    public void test_format_with_windows_control_characters() {
        Assert.assertEquals("this\r\nis a string\r\nwith mixed control characters\r\n"
            , ControlCharacterUtils.format("this\r\nis a string\r\nwith mixed control characters\r\n"
                , ControlCharacterUtils.ControlCharacter.WINDOWS));

        Assert.assertEquals("this\nis a string\nwith mixed control characters\n"
            , ControlCharacterUtils.format("this\r\nis a string\r\nwith mixed control characters\r\n"
                , ControlCharacterUtils.ControlCharacter.UNIX));
    }

    @Test
    public void test_format_with_unix_control_characters() {
        Assert.assertEquals("this\r\nis a string\r\nwith mixed control characters\r\n"
            , ControlCharacterUtils.format("this\nis a string\nwith mixed control characters\n"
                , ControlCharacterUtils.ControlCharacter.WINDOWS));

        Assert.assertEquals("this\nis a string\nwith mixed control characters\n"
            , ControlCharacterUtils.format("this\nis a string\nwith mixed control characters\n"
                , ControlCharacterUtils.ControlCharacter.UNIX));
    }

}
