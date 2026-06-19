package com.mtc.mutuaConseil.services.servicesImpl;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.services.servicesImpl.parametrage.CompteService;
import de.taimos.totp.TOTP;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URI;

@Service
public class AuthenticatorService {
    @Autowired
    private CompteService compteService;

    public String getTOTPCode(String secretKey) {
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secretKey);
        String hexKey = Hex.encodeHexString(bytes);
        return TOTP.getOTP(hexKey);
    }

    public String getSecretKey(Compte c, String path) throws Exception {
        // Récupérer le compte depuis le service
        Compte compte = compteService.getCompteFromId(c.getId());
        // Si la clé est déjà présente dans le compte, la retourner
        if (compte != null && compte.getAuthKeyQrCode() != null && !compte.getAuthKeyQrCode().isEmpty()) {
            return compte.getAuthKeyQrCode();
        } else {
            // Vérifier si le fichier QR code existe
            File qrCodeFile = new File(path);
            if (!qrCodeFile.exists()) {
                throw new FileNotFoundException("Le fichier QR code est introuvable : " + path);
            }
            try {
                // Lire l'image du fichier
                BufferedImage bufferedImage = ImageIO.read(qrCodeFile);
                if (bufferedImage == null) {
                    throw new IOException("Impossible de lire l'image du fichier : " + path);
                }
                BufferedImage grayscaleImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
                Graphics g = grayscaleImage.getGraphics();
                g.drawImage(bufferedImage, 0, 0, null);
                g.dispose();
                // Décoder le QR code
                LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                Result result = new MultiFormatReader().decode(bitmap);
                String qrCodeText = result.getText();
                // Extraire la clé secrète de l'URL otpauth
                String secretKey = extractSecretKeyFromQRCode(qrCodeText);
                if (secretKey != null) {
                    // Mettre à jour la clé secrète dans le compte
                    compteService.updateCompte(c.getId(), secretKey);
                }
                return secretKey;
            } catch (NotFoundException e) {
                throw new Exception("QR Code introuvable ou illisible dans l'image : " + path, e);
            } catch (IOException e) {
                throw new Exception("Erreur lors de la lecture de l'image : " + path, e);
            }
        }
    }

    private String extractSecretKeyFromQRCode(String qrCodeText) throws Exception {
        URI uri = new URI(qrCodeText);
        String query = uri.getQuery(); // "secret=ABC123DEF456&issuer=VotreApp"
        String[] params = query.split("&");
        for (String param : params) {
            if (param.startsWith("secret=")) {
                return param.split("=")[1];
            }
        }
        throw new Exception("Clé secrète non trouvée dans le QR code");
    }

//    public String getSecretKey(Compte c, String pdfPath) throws Exception {
//        Compte compte = compteService.getCompteFromId(c.getId());
//
//        if (compte != null && compte.getAuthKeyQrCode() != null && !compte.getAuthKeyQrCode().isEmpty()) {
//            return compte.getAuthKeyQrCode();
//        } else {
//            // Définir le chemin de l'image de sortie
//            String outputImagePath = "qrcode.png"; // ou un autre chemin approprié
//
//            // Extraire l'image du PDF
//            try {
//                this.extractImagesFromPDF(pdfPath, outputImagePath);
//            } catch (IOException e) {
//                throw new Exception("Erreur lors de l'extraction de l'image du PDF : " + e.getMessage(), e);
//            }
//
//            // Lire l'image extraite
//            BufferedImage bufferedImage = ImageIO.read(new File(outputImagePath));
//            if (bufferedImage == null) {
//                throw new IOException("Impossible de lire l'image extraite : " + outputImagePath);
//            }
//
//            // Décoder le QR code
//            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
//            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
//
//            try {
//                Result result = new MultiFormatReader().decode(bitmap);
//                String qrCodeText = result.getText(); // URL otpauth
//
//                // Extraire la clé secrète de l'URL otpauth
//                String secretKey = extractSecretKeyFromQRCode(qrCodeText);
//                if (secretKey != null) {
//                    compteService.updateCompte(c.getId(), secretKey);
//                }
//                return secretKey;
//            } catch (NotFoundException e) {
//                throw new Exception("QR Code introuvable ou illisible dans l'image extraite : " + outputImagePath, e);
//            }
//        }
//    }

}
