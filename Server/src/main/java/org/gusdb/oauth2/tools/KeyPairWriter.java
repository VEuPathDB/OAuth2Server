package org.gusdb.oauth2.tools;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPublicKey;
import java.util.Calendar;
import java.util.Date;

import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcECContentSignerBuilder;
import org.gusdb.oauth2.exception.CryptoException;
import org.gusdb.oauth2.shared.ECPublicKeyRepresentation;
import org.gusdb.oauth2.shared.SigningKeyStore;

/**
 * This program generates a keystore file at the passed location containing a pair
 * of asymmetric elliptic curve keys, using the passed seed and secured with an
 * optional passphrase.
 *
 * Note: seed value does NOT generate the same keys i.e. key generation is not
 * deterministic given the same passed arguments.
 */
public class KeyPairWriter {

  // constants for certificate attached to key storage
  private static final String SELF_SIGN_CERT_DN = "CN=VEuPathDB-OAuth";
  public static final String SELF_SIGN_CERT_ENTRY_NAME = "OAuth";

  // 3 year validity; may change keys before that
  private static final int SELF_SIGN_CERT_EXP_LENGTH = 3;
  private static final int SELF_SIGN_CERT_EXP_UNITS = Calendar.YEAR;

  private static final String REQUIRED_FILENAME_EXTENSION = ".pkcs12";

  public static void main(String[] args) {
    try {
      if (args.length != 3) {
        System.err.println("\nUSAGE: java " + KeyPairWriter.class.getName() + " <outputFile> <passPhrase> <seed>\n");
        System.exit(1);
      }
      Path outputFile = Paths.get(args[0]);
      if (!outputFile.getFileName().toString().endsWith(REQUIRED_FILENAME_EXTENSION)) {
        throw new IllegalArgumentException("outputFile must use extension: " + REQUIRED_FILENAME_EXTENSION);
      }
      String passPhrase = args[1];
      String seed = args[2];
      new KeyPairWriter().writeKeyPair(outputFile, passPhrase, seed);
    }
    catch (CryptoException | IOException e) {
      System.err.println("Unable to generate or write keys");
      e.printStackTrace(System.err);
      System.exit(2);
    }
  }

  private void writeKeyPair(Path outputFile, String passPhrase, String seed)
      throws CryptoException, IOException {

    KeyPair keyPair = new SigningKeyStore(seed).getAsyncKeys();
    try {
      ECPublicKeyRepresentation keyRep = new ECPublicKeyRepresentation((ECPublicKey)keyPair.getPublic());
      System.out.println("Stored public key:\n" + keyRep.getBase64String());
  
      Certificate selfSignedCertificate = getSelfSignedCert(keyPair, SELF_SIGN_CERT_DN);
  
      KeyStore pkcs12KeyStore = KeyStore.getInstance("PKCS12");
      pkcs12KeyStore.load(null, null);
  
      KeyStore.Entry entry = new PrivateKeyEntry(keyPair.getPrivate(), new Certificate[] { selfSignedCertificate });
      KeyStore.ProtectionParameter param = new KeyStore.PasswordProtection(passPhrase.toCharArray());
  
      pkcs12KeyStore.setEntry(SELF_SIGN_CERT_ENTRY_NAME, entry, param);
  
      try (FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
        pkcs12KeyStore.store(fos, passPhrase.toCharArray());
      }
    }
    catch (InvalidKeyException | OperatorCreationException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
      throw new CryptoException("Unable to generate or sign key pair", e);
    }
  }

  public Certificate getSelfSignedCert(KeyPair keyPair, String subjectDN)
      throws OperatorCreationException, CertificateException, InvalidKeyException {

    Provider bcProvider = new BouncyCastleProvider();
    Security.addProvider(bcProvider);

    Date[] validTimeWindow = getValidTimeWindow();

    X500Name dnName = new X500Name(SELF_SIGN_CERT_DN);

    // Using the current timestamp as the certificate serial number
    BigInteger certSerialNumber = BigInteger.valueOf(validTimeWindow[0].getTime());

    SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(
        keyPair.getPublic().getEncoded());

    X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(
        dnName, certSerialNumber, validTimeWindow[0], validTimeWindow[1], dnName, subjectPublicKeyInfo);

    ContentSigner contentSigner = new BcECContentSignerBuilder(
        new AlgorithmIdentifier(NISTObjectIdentifiers.id_ecdsa_with_sha3_512),
        new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha512)
    ).build(ECUtil.generatePrivateKeyParameter(keyPair.getPrivate()));

    X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);
    Certificate selfSignedCert = new JcaX509CertificateConverter().getCertificate(certificateHolder);

    return selfSignedCert;
  }

  private Date[] getValidTimeWindow() {
    Date startDate = new Date();
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(startDate);
    calendar.add(SELF_SIGN_CERT_EXP_UNITS, SELF_SIGN_CERT_EXP_LENGTH);
    Date endDate = calendar.getTime();
    return new Date[] { startDate, endDate };
  }

}
