package com.example.doc_management_syst.controllers;

import com.example.doc_management_syst.models.User;
import com.example.doc_management_syst.services.FileContentExtractor;
import com.example.doc_management_syst.services.UserService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.example.interfaces.DocumentClassifier;
import org.example.interfaces.DocumentSummarizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/drive")
public class DriverController {

    private static final String STORAGE_BASE_PATH = "storage";
    private final UserService userService;

    @Autowired
    private DocumentClassifier documentClassifier;

    @Autowired
    private DocumentSummarizer documentSummarizer;

    public DriverController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String showDrive(@RequestParam(required = false, defaultValue = "") String path, Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        Path userBasePath = Paths.get(STORAGE_BASE_PATH, user.getUsername());
        System.out.println(userBasePath);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        Path currentPath = userBasePath.resolve(path).normalize();

        System.out.println(path);
        System.out.println(currentPath);


        try {
            Files.createDirectories(userBasePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create user base directory", e);
        }

        File[] files = currentPath.toFile().listFiles();
        List<String> folders = null;
        List<String> documents = null;

        if (files != null) {
            try {
                folders = getExistingFolders(currentPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                documents = getDocuments(currentPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println(path);
        model.addAttribute("relativePath", path);  // Pass sanitized path for navigation
        if(path.isEmpty()||path.charAt(0)!='/')
            path='/'+path;

        model.addAttribute("currentPath", path);  // Pass resolved path
        model.addAttribute("folders", folders);
        model.addAttribute("documents", documents);
        model.addAttribute("user", user);

        return "drive";
    }
    private void addDriveAttributesToModel(ModelAndView model, String currentPath, Path userBasePath, User user) {
        try {
            Path currentFullPath = userBasePath.resolve(currentPath).normalize();
            model.addObject("user", user);
            model.addObject("currentPath", "/"+currentPath);
            model.addObject("relativePath", currentPath);
            model.addObject("folders", getExistingFolders(currentFullPath));
            model.addObject("documents", getDocuments(currentFullPath));
        } catch (IOException e) {
            model.addObject("errorMessage", "Error: Could not fetch folder or document details.");
        }
    }


    @PostMapping("/classify")
    public ModelAndView classifyDocument(@RequestParam String content, @RequestParam String folders,
                                   @RequestParam String currentPath, Authentication authentication) {

        ModelAndView model = new ModelAndView();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Path userBasePath = Paths.get(STORAGE_BASE_PATH, user.getUsername());
        try {

            Path documentPath = userBasePath.resolve(currentPath).resolve(content);

            String documentContent = preprocessContent(FileContentExtractor.extractFileContent(documentPath));


            if (Objects.equals(folders, "[]")) {
                model.addObject("errorMessage", "Error: At least one folder must be specified for classification.");
                addDriveAttributesToModel(model, currentPath, userBasePath, user);
                model.setViewName("drive");
                return model;
            }

            String classification = documentClassifier.classify(documentContent, folders);

            Path classifiedFolderPath = userBasePath.resolve(classification);
            Files.createDirectories(classifiedFolderPath);
            Files.move(documentPath, classifiedFolderPath.resolve(content), StandardCopyOption.REPLACE_EXISTING);

            model.addObject("classificationMessage", "Document classified into folder: " + classification);
            addDriveAttributesToModel(model,currentPath,userBasePath,user);
        } catch (IOException e) {
            model.addObject("errorMessage", "Error: Could not classify the document. " + e.getMessage());
            addDriveAttributesToModel(model,currentPath,userBasePath,user);

        }

        model.setViewName("drive");
        return model;
    }
    private List<String> getExistingFolders(Path userBasePath) throws IOException {
        File[] files = userBasePath.toFile().listFiles();
        if (files != null) {
            return Arrays.stream(files)
                    .filter(File::isDirectory)
                    .map(File::getName)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private List<String> getDocuments(Path currentPath) throws IOException {
        File[] files = currentPath.toFile().listFiles();
        if (files != null) {
            return Arrays.stream(files)
                    .filter(File::isFile)
                    .map(File::getName)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }



    private static String preprocessContent(String content) {
        String processedContent = content.replaceAll("\\s+", " ").trim();
        return processedContent.length() > 4000 ? processedContent.substring(0, 4000) : processedContent;
    }


    @PostMapping("/summarize")
    public String summarizeDocument(@RequestParam String content, @RequestParam String currentPath,
                                    Authentication authentication, Model model) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            Path userBasePath = Paths.get(STORAGE_BASE_PATH, user.getUsername());
            Path documentPath = userBasePath.resolve(currentPath).resolve(content);

            String documentContent = preprocessContent(FileContentExtractor.extractFileContent(documentPath));

            String summary = documentSummarizer.summarize(documentContent);

            model.addAttribute("summary", summary);
            model.addAttribute("documentName", content);
            model.addAttribute("currentPath", currentPath);
        } catch (IOException e) {
            model.addAttribute("errorMessage", "Error: Could not summarize the document. " + e.getMessage());
            return "redirect:/drive?path=" + currentPath;
        }
        return "summary";
    }


    @PostMapping("/add-folder")
    public ModelAndView addFolder(@RequestParam String name, @RequestParam String currentPath, Authentication authentication, ModelAndView model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Path userBasePath = Paths.get(STORAGE_BASE_PATH, user.getUsername());

        if (name == null || name.trim().isEmpty()) {
            model.addObject("errorMessage", "Folder name cannot be empty.");
            addDriveAttributesToModel(model,currentPath,userBasePath,user);

            model.setViewName("drive");
            return model;
        }
        if (name.contains(" ")) {
            model.addObject("errorMessage", "Folder name must be a single word, no spaces allowed.");
            addDriveAttributesToModel(model, currentPath, userBasePath, user);
            model.setViewName("drive");
            return model;
        }


        Path newFolderPath = userBasePath.resolve(currentPath).resolve(name);

        try {
            Files.createDirectories(newFolderPath);
        } catch (IOException e) {
            model.addObject("errorMessage", "Could not create folder: " + e.getMessage());
            model.setViewName("drive");
            addDriveAttributesToModel(model,currentPath,userBasePath,user);
            return model;
        }

        model.setViewName("drive");
        addDriveAttributesToModel(model,currentPath,userBasePath,user);
        return model;
    }

    @PostMapping("/upload-document")
    public ModelAndView uploadDocument(@RequestParam("file") MultipartFile file, @RequestParam String currentPath, Authentication authentication) {
        ModelAndView modelAndView = new ModelAndView();

        String username = authentication.getName();
        User user = userService.findByUsername(username);

        Path userBasePath = Paths.get(STORAGE_BASE_PATH, user.getUsername());
        Path normalizedCurrentPath = currentPath == null || currentPath.isEmpty()
                ? userBasePath
                : userBasePath.resolve(currentPath).normalize();

        Path destinationPath = normalizedCurrentPath.resolve(file.getOriginalFilename());

        if (file.isEmpty()) {
            modelAndView.addObject("errorMessage", "File cannot be empty.");
            addDriveAttributesToModel(modelAndView, currentPath, userBasePath, user);
            modelAndView.setViewName("drive");
            return modelAndView;
        }

        try {
            Files.createDirectories(destinationPath.getParent());
            file.transferTo(destinationPath);
        } catch (IOException e) {
            modelAndView.addObject("errorMessage", "Could not upload file: " + e.getMessage());
            addDriveAttributesToModel(modelAndView, currentPath, userBasePath, user);
            modelAndView.setViewName("drive");
            return modelAndView;
        }

        modelAndView.setViewName("redirect:/drive?path=" + currentPath);
        return modelAndView;
    }

    @PostMapping("/delete")
    public String deleteItem(@RequestParam String name, @RequestParam String currentPath, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        Path userBasePath = Paths.get(STORAGE_BASE_PATH, user.getUsername());
        Path itemPath = userBasePath.resolve(currentPath).resolve(name);

        try {
            if (Files.isDirectory(itemPath)) {
                Files.walk(itemPath)
                        .sorted((path1, path2) -> path2.compareTo(path1))
                        .forEach(path -> path.toFile().delete());
            } else {
                Files.deleteIfExists(itemPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not delete item", e);
        }

        return "redirect:/drive?path=" + currentPath;
    }
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadDocument(@RequestParam String name, @RequestParam String currentPath, Authentication authentication) throws IOException {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Path userBasePath = Paths.get(STORAGE_BASE_PATH, user.getUsername());
        Path documentPath = userBasePath.resolve(Paths.get(currentPath)).resolve(name);

        if (!Files.exists(documentPath)) {
            throw new FileNotFoundException("Document not found: " + name);
        }

        Resource resource = new UrlResource(documentPath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException("Could not read the file: " + name);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .body(resource);
    }
    @GetMapping("/view")
    public ModelAndView viewDocument(@RequestParam String name, @RequestParam String currentPath, Authentication authentication) throws IOException {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Path userBasePath = Paths.get(STORAGE_BASE_PATH, user.getUsername());
        Path documentPath;
        if (currentPath == null || currentPath.isEmpty() || currentPath.equals("/")) {
            documentPath = userBasePath.resolve(name);
        } else {
            documentPath = userBasePath.resolve(Paths.get(currentPath)).resolve(name);
        }

        if (!Files.exists(documentPath)) {
            throw new FileNotFoundException("Document not found: " + name);
        }

        ModelAndView model = new ModelAndView();
        model.addObject("documentName", name);
        model.addObject("currentPath", currentPath);

        String mimeType = Files.probeContentType(documentPath);
        System.out.println(documentPath);

        if (mimeType != null && mimeType.startsWith("text")) {
            String documentContent = Files.readString(documentPath);
            model.addObject("documentContent", documentContent);
            model.setViewName("view-document-text");
        } else if (mimeType != null && mimeType.equals("application/pdf")) {
            model.addObject("documentPath", "/drive/view-file?name=" + name + "&currentPath=" + currentPath);
            model.setViewName("view-document-pdf");
        } else if (mimeType != null && mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            try (FileInputStream fis = new FileInputStream(documentPath.toFile());
                 XWPFDocument doc = new XWPFDocument(fis)) {
                StringBuilder documentContent = new StringBuilder();
                doc.getParagraphs().forEach(paragraph -> documentContent.append(paragraph.getText()).append("\n"));
                model.addObject("documentContent", documentContent.toString());
            }
            model.setViewName("view-document-word");
        } else if (mimeType != null && mimeType.startsWith("image")) {
            model.addObject("documentPath", "/drive/view-file?name=" + name + "&currentPath=" + currentPath);
            model.setViewName("view-document-image");
        } else {
            model.addObject("errorMessage", "Unsupported file type. You can download the file instead.");
            model.setViewName("view-document-error");
        }

        return model;
    }

    @GetMapping("/view-file")
    public ResponseEntity<Resource> viewFile(@RequestParam String name, @RequestParam String currentPath, Authentication authentication) throws IOException {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Path userBasePath = Paths.get(STORAGE_BASE_PATH, user.getUsername());
        Path documentPath = userBasePath.resolve(currentPath).resolve(name);

        if (!Files.exists(documentPath)) {
            throw new FileNotFoundException("Document not found: " + name);
        }

        Resource resource = new UrlResource(documentPath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException("Could not read the file: " + name);
        }


        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(Files.probeContentType(documentPath)))
                .body(resource);
    }

}

