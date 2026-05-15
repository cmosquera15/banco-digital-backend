package co.edu.udea.bancodigital.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;
    private static final String FROM_EMAIL = "noreply@bancodigital.com";

    /**
     * Send a simple text email
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_EMAIL);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Simple email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Error sending simple email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Error sending email", e);
        }
    }

    /**
     * Send an HTML email
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(FROM_EMAIL);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML content
            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Error sending HTML email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Error sending email", e);
        }
    }

    /**
     * Send registration confirmation email
     */
    public void sendRegistrationConfirmation(String to, String nombreUsuario) {
        String subject = "Bienvenido a Banco Digital";
        String htmlContent = String.format("""
                <html>
                    <body>
                        <h2>¡Bienvenido a Banco Digital!</h2>
                        <p>Hola %s,</p>
                        <p>Tu cuenta ha sido registrada exitosamente.</p>
                        <p>Ya puedes acceder a tu cuenta con tus credenciales.</p>
                        <br>
                        <p>Saludos,<br>Equipo Banco Digital</p>
                    </body>
                </html>
                """, nombreUsuario);
        sendHtmlEmail(to, subject, htmlContent);
    }

    /**
     * Send password reset email
     */
    public void sendPasswordReset(String to, String resetToken) {
        String subject = "Recuperar contraseña - Banco Digital";
        String resetLink = "https://bancodigital.com/reset-password?token=" + resetToken;
        String htmlContent = String.format("""
                <html>
                    <body>
                        <h2>Recuperar Contraseña</h2>
                        <p>Hemos recibido una solicitud para recuperar tu contraseña.</p>
                        <p>Haz clic en el siguiente enlace para restablecer tu contraseña:</p>
                        <p><a href="%s">Restablecer Contraseña</a></p>
                        <p>Este enlace expirará en 24 horas.</p>
                        <p>Si no solicitaste este cambio, ignora este correo.</p>
                        <br>
                        <p>Saludos,<br>Equipo Banco Digital</p>
                    </body>
                </html>
                """, resetLink);
        sendHtmlEmail(to, subject, htmlContent);
    }

    /**
     * Send transaction notification email
     */
    public void sendTransactionNotification(String to, String tipoTransaccion, String monto, String cuenta) {
        String subject = "Notificación de Transacción - Banco Digital";
        String htmlContent = String.format("""
                <html>
                    <body>
                        <h2>Notificación de Transacción</h2>
                        <p>Se ha realizado una %s en tu cuenta.</p>
                        <p><strong>Detalles:</strong></p>
                        <ul>
                            <li>Tipo: %s</li>
                            <li>Monto: $%s</li>
                            <li>Cuenta: %s</li>
                            <li>Fecha: %s</li>
                        </ul>
                        <p>Si no reconoces esta transacción, contacta a nuestro equipo de soporte.</p>
                        <br>
                        <p>Saludos,<br>Equipo Banco Digital</p>
                    </body>
                </html>
                """, tipoTransaccion.toLowerCase(), tipoTransaccion, monto, cuenta, java.time.LocalDateTime.now());
        sendHtmlEmail(to, subject, htmlContent);
    }

    /**
     * Send account alert email
     */
    public void sendAccountAlert(String to, String alertMessage) {
        String subject = "Alerta de Seguridad - Banco Digital";
        String htmlContent = String.format("""
                <html>
                    <body>
                        <h2>Alerta de Seguridad</h2>
                        <p>Hemos detectado una actividad en tu cuenta que requiere tu atención:</p>
                        <p><strong>%s</strong></p>
                        <p>Si esta actividad no fue realizada por ti, cambia tu contraseña inmediatamente.</p>
                        <br>
                        <p>Saludos,<br>Equipo Banco Digital</p>
                    </body>
                </html>
                """, alertMessage);
        sendHtmlEmail(to, subject, htmlContent);
    }
}
