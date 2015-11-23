package com.mail163.email.mail.transport;

import java.net.URI;
import java.net.URISyntaxException;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Simple unit tests for MailSender.  Tests here should not attempt any actual connections.
 */
@SmallTest
public class MailTransportUnitTests extends AndroidTestCase {

    /**
     * Tests of the Uri parsing logic
     */
    public void _testUriParsing() throws URISyntaxException {

        // Parse with everything in the Uri
        URI uri = new URI("smtp://user:password@server.com:999");
        MailTransport transport = new MailTransport("SMTP");
        transport.setUri(uri, 888);
        assertEquals("server.com", transport.getHost());
        assertEquals(999, transport.getPort());
        String[] userInfoParts = transport.getUserInfoParts();
        assertNotNull(userInfoParts);
        assertEquals("user", userInfoParts[0]);
        assertEquals("password", userInfoParts[1]);

        // Parse with no user/password (e.g. anonymous SMTP)
        uri = new URI("smtp://server.com:999");
        transport = new MailTransport("SMTP");
        transport.setUri(uri, 888);
        assertEquals("server.com", transport.getHost());
        assertEquals(999, transport.getPort());
        assertNull(transport.getUserInfoParts());
    }
}
