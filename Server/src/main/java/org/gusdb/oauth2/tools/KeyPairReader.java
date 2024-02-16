package org.gusdb.oauth2.tools;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import org.gusdb.oauth2.exception.CryptoException;
import org.gusdb.oauth2.shared.ECPublicKeyRepresentation;

public class KeyPairReader {

  public static void main(String[] args) {
    try {
      if (args.length != 2) {
        System.err.println("\nUSAGE: java " + KeyPairReader.class.getName() + " <inputFile> <passPhrase>\n");
        System.exit(1);
      }
      Path inputFile = Paths.get(args[0]);
      String passPhrase = args[1];
      KeyPair keyPair = new KeyPairReader().readKeyPair(inputFile, passPhrase);
      ECPublicKeyRepresentation keyRep = new ECPublicKeyRepresentation((ECPublicKey)keyPair.getPublic());
      System.out.println("Stored public key:\n" + keyRep.getBase64String());
    }
    catch (IOException | CryptoException  e) {
      
    }
  }

  public KeyPair readKeyPair(Path inputFile, String passPhrase) throws IOException, CryptoException {

    try {
      KeyStore pkcs12KeyStore = KeyStore.getInstance("PKCS12");
  
      try (FileInputStream fis = new FileInputStream(inputFile.toFile());) {
        pkcs12KeyStore.load(fis, passPhrase.toCharArray());
      }
    
      KeyStore.ProtectionParameter param = new KeyStore.PasswordProtection(passPhrase.toCharArray());
      Entry entry = pkcs12KeyStore.getEntry(KeyPairWriter.SELF_SIGN_CERT_ENTRY_NAME, param);
      if (!(entry instanceof PrivateKeyEntry)) {
        throw new KeyStoreException("That's not a private key!");
      }
      PrivateKeyEntry privKeyEntry = (PrivateKeyEntry) entry;

      // cast just to prove we can; want to generate ClassCastException here if not
      ECPublicKey publicKey = (ECPublicKey)privKeyEntry.getCertificate().getPublicKey();
      ECPrivateKey privateKey = (ECPrivateKey)privKeyEntry.getPrivateKey();

      return new KeyPair(publicKey, privateKey);
    }
    catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException | ClassCastException e) {
      throw new CryptoException("Unable to produce or verify EC keys from key store file", e);
    }
  }
}
