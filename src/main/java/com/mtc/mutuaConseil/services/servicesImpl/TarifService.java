package com.mtc.mutuaConseil.services.servicesImpl;

import com.mtc.mutuaConseil.models.ParametreGeneral;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.repositories.FluxDataRepository;
import com.mtc.mutuaConseil.repositories.TarifRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TarifService {

    private final static Logger log = LoggerFactory.getLogger(TarifService.class);
    private final TarifRepository tarifRepository;
    private final ParametreGeneralService parametreGeneralService;

    public TarifService(TarifRepository tarifRepository, ParametreGeneralService parametreGeneralService){
        this.tarifRepository = tarifRepository;
        this.parametreGeneralService = parametreGeneralService;
    }

    // Récupérer un Tarif par son ID
    public Tarif findTarifById(Long id) {
        return tarifRepository.findById(id).orElseThrow(() -> new RuntimeException("Tarif non trouvé"));
    }

    // Récupérer tous les Tarifs
    public List<Tarif> findAllTarifs() {
        return tarifRepository.findAll();
    }

    // Mettre à jour un Tarif
    @Transactional
    public Tarif updateTarif(Long id, Tarif tarifDetails) {
        Tarif tarif = findTarifById(id);
        tarif.setNom(tarifDetails.getNom());
        tarif.setMontant(tarifDetails.getMontant());
        tarif.setTypeAssurance(tarifDetails.getTypeAssurance());
        tarif.setExecution(tarifDetails.isExecution());
        tarif.setErreur(tarifDetails.getErreur());
        tarif.setEtape(tarifDetails.getEtape());
        return tarifRepository.save(tarif);
    }

    // Supprimer un Tarif
    @Transactional
    public void deleteTarif(Long id) {
        Tarif tarif = findTarifById(id);
        tarifRepository.delete(tarif);
        // Supprimer les fichiers d'images associés
        deleteImageFiles(tarif);
    }

    public String saveImage(BufferedImage image, String nom, String tempId, boolean isError) throws IOException {
        // Définition du chemin du dossier
        ParametreGeneral parametreGeneral = parametreGeneralService.findById(1L);
        String baseDir = parametreGeneral.getDossierImage();
        String subDir = isError ? "\\imgErreurs" : "\\imgTarifs";

        // Date du jour pour organiser les fichiers
        String dateFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String directoryPath = baseDir + subDir + "\\" + dateFolder;

        // Création du dossier si nécessaire
        Files.createDirectories(Paths.get(directoryPath));

        // Nom du fichier
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = tempId + "_" + nom + "_" + dateTime + ".png";
        String filePath = directoryPath + "\\" + fileName;

        // Sauvegarde de l’image
        File outputFile = new File(filePath);
        ImageIO.write(image, "png", outputFile);

        return filePath; // Retourne le chemin de l'image sauvegardée
    }

    public String saveImageByte(byte[] screenshotBytes, String nom, String tempId, boolean isError) {
        try {
            // Convertir le tableau d'octets en BufferedImage
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(screenshotBytes));
            // Appeler la méthode de sauvegarde d'image
            return saveImage(image, nom, tempId, isError);
        } catch (Exception e) {
            log.error("Erreur_ saveImage : {} ", e.getMessage());
            return null;
        }
    }

    // Renommer les fichiers avec l'ID réel après la persistance
    private void renameImageFiles(Tarif tarif) {
        renameFile(tarif.getCaptureImg(), tarif.getId());
        renameFile(tarif.getCaptureImgErreur(), tarif.getId());
    }

    // Renommer un fichier spécifique
    private void renameFile(String filePath, Long newId) {
        Path currentPath = Paths.get(filePath);
        String currentFileName = currentPath.getFileName().toString();
        // Regex pour identifier l'UUID dans le nom du fichier
        String uuidRegex = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
        Pattern pattern = Pattern.compile(uuidRegex);
        Matcher matcher = pattern.matcher(currentFileName);
        // Remplacer l'UUID par l'ID de Tarif
        String newFileName = "";
        if (matcher.find()) {
            newFileName = currentFileName.replaceFirst(uuidRegex, newId.toString());
        } else {
            log.info("Le nom du fichier ne contient pas d'UUID valide...");
        }
        // Nouveau chemin basé sur le nouveau nom de fichier
        Path newPath = currentPath.getParent().resolve(newFileName);
        try {
            // Renommer le fichier
            Files.move(currentPath, newPath);
        } catch (IOException e) {
            log.info("Erreur lors du roomage du fichier {} ", e.getMessage());
        }
    }

    // Supprimer les fichiers d'images associés
    private void deleteImageFiles(Tarif tarif) {
        deleteFile(tarif.getCaptureImg());
        deleteFile(tarif.getCaptureImgErreur());
    }

    // Supprimer un fichier spécifique
    private void deleteFile(String filePath) {
        if (filePath != null) {
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (IOException e) {
                log.info("Erreur lors de la suppression du fichier {} ", e.getMessage());
            }
        }
    }

}
