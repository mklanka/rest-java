package com.incept5.rest.service;


import com.incept5.rest.user.api.ExternalUser;
import com.incept5.rest.config.ApplicationConfig;
import com.incept5.rest.user.domain.Role;
import com.incept5.rest.user.domain.User;
import com.incept5.rest.user.mail.MailSenderService;
import com.incept5.rest.user.mail.MockJavaMailSender;
import com.incept5.rest.user.service.data.EmailServiceTokenModel;
import com.incept5.rest.user.mail.impl.MailSenderServiceImpl;
import com.incept5.rest.user.domain.VerificationToken;
import org.apache.commons.codec.binary.Base64;
import org.apache.velocity.app.VelocityEngine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author: Iain Porter
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:META-INF/spring/root-context.xml")
@ActiveProfiles(profiles = "dev")
@Transactional
public class MailSenderServiceTest extends BaseServiceTest {

    private MailSenderService mailService;

    private MockJavaMailSender mailSender;

    @Autowired
    VelocityEngine velocityEngine;

    @Autowired
    ApplicationConfig config;

    @Before
    public void setUpServices() {
        mailSender = new MockJavaMailSender();
        mailService = new MailSenderServiceImpl(mailSender, velocityEngine);
        ((MailSenderServiceImpl)mailService).setConfig(config);
    }


    @Test
    public void sendVerificationEmail() throws Exception {
        ExternalUser externalUser = createUserWithRandomUserName(Role.authenticated);
        User user = userRepository.findByUuid(externalUser.getId());
        VerificationToken token = new VerificationToken(user,
                VerificationToken.VerificationTokenType.emailVerification, 120);
        mailService.sendVerificationEmail(new EmailServiceTokenModel(user, token, config.getHostNameUrl()));
        assertOnMailResult(user, token);
    }

    @Test
    public void sendRegistrationEmail() throws Exception {
        ExternalUser externalUser = createUserWithRandomUserName(Role.authenticated);
        User user = userRepository.findByUuid(externalUser.getId());
        VerificationToken token = new VerificationToken(user,
                VerificationToken.VerificationTokenType.emailRegistration, 120);
        mailService.sendRegistrationEmail(new EmailServiceTokenModel(user, token, config.getHostNameUrl()));
        assertOnMailResult(user, token);
    }

    @Test
    public void sendLostPasswordEmail() throws Exception {
        ExternalUser externalUser = createUserWithRandomUserName(Role.authenticated);
        User user = userRepository.findByUuid(externalUser.getId());
        VerificationToken token = new VerificationToken(user,
                VerificationToken.VerificationTokenType.lostPassword, 120);
        mailService.sendLostPasswordEmail(new EmailServiceTokenModel(user, token, config.getHostNameUrl()));
        assertOnMailResult(user, token);
    }

    private void assertOnMailResult(User user, VerificationToken token) throws MessagingException, IOException {
        List<MimeMessage> messages = mailSender.getMessages();
        assertThat(messages.size(), is(1));
        MimeMessage message = messages.get(0);
        assertThat(message.getAllRecipients()[0].toString(), is((user.getEmailAddress())));
        Multipart multipart = (Multipart)message.getContent();
        String content = (String)multipart.getBodyPart(0).getContent();
        assertThat(content, containsString(new String(Base64.encodeBase64(token.getToken().getBytes()))));
    }


}
