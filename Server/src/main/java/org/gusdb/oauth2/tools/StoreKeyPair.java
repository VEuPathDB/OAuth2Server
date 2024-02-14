package org.gusdb.oauth2.tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

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
import org.gusdb.oauth2.shared.SigningKeyStore;

public class StoreKeyPair {

  public static Certificate selfSign(KeyPair keyPair, String subjectDN)
      throws OperatorCreationException, CertificateException, InvalidKeyException {
    Provider bcProvider = new BouncyCastleProvider();
    Security.addProvider(bcProvider);

    long now = System.currentTimeMillis();
    Date startDate = new Date(now);

    X500Name dnName = new X500Name(subjectDN);

    // Using the current timestamp as the certificate serial number
    BigInteger certSerialNumber = new BigInteger(Long.toString(now));

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(startDate);
    // 3 year validity; may change keys before that
    calendar.add(Calendar.YEAR, 3);

    Date endDate = calendar.getTime();

    // Use appropriate signature algorithm based on your keyPair algorithm.
    //String signatureAlgorithm = "SHA256WithRSA";

    SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(
        keyPair.getPublic().getEncoded());

    X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(dnName, certSerialNumber,
        startDate, endDate, dnName, subjectPublicKeyInfo);

    ContentSigner contentSigner = new BcECContentSignerBuilder(
        new AlgorithmIdentifier(NISTObjectIdentifiers.id_ecdsa_with_sha3_512),
        new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha512)
    ).build(ECUtil.generatePrivateKeyParameter(keyPair.getPrivate()));

    /*
    ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm)
        .setProvider(bcProvider)
        .build(keyPair.getPrivate());*/

    X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);

    Certificate selfSignedCert = new JcaX509CertificateConverter().getCertificate(certificateHolder);

    return selfSignedCert;
  }

  public static void main(String[] args) throws Exception {

    KeyPair generatedKeyPair = new SigningKeyStore("ne2OCyFSoXXtLCR2RQuUsaqaWBmnwufNNhCyv6KygkwDDpILeOv67MEecKguBFrhqyiYO/UM6JJzVd5Xh3JwSA==").getAsyncKeys();

    String filename = "test_gen_self_signed.pkcs12";
    char[] password = "test".toCharArray();

    storeToPKCS12(filename, password, generatedKeyPair);

    KeyPair retrievedKeyPair = loadFromPKCS12(filename, password);

    // you can validate by generating a signature and verifying it or by
    // comparing the moduli by first casting to RSAPublicKey, e.g.:

    ECPublicKey pubKey = (ECPublicKey) generatedKeyPair.getPublic();
    ECPrivateKey privKey = (ECPrivateKey) retrievedKeyPair.getPrivate();

    // verify things work!

    //KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
    //keyGen.initialize(2048);

    //KeyPair keyPair = keyGen.generateKeyPair();
    //PublicKey publicKey = keyPair.getPublic();
    //PrivateKey privateKey = keyPair.getPrivate();

    // create a challenge
    byte[] challenge = new byte[10000];
    ThreadLocalRandom.current().nextBytes(challenge);

    // sign using the private key
    Signature sig = Signature.getInstance("SHA512withECDSA");
    sig.initSign(retrievedKeyPair.getPrivate());
    sig.update(challenge);
    byte[] signature = sig.sign();

    // verify signature using the public key
    sig.initVerify(generatedKeyPair.getPublic());
    sig.update(challenge);

    System.out.println(sig.verify(signature));
  }

  private static KeyPair loadFromPKCS12(String filename, char[] password)
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException,
      IOException, UnrecoverableEntryException {
    KeyStore pkcs12KeyStore = KeyStore.getInstance("PKCS12");

    try (FileInputStream fis = new FileInputStream(filename);) {
      pkcs12KeyStore.load(fis, password);
    }

    KeyStore.ProtectionParameter param = new KeyStore.PasswordProtection(password);
    Entry entry = pkcs12KeyStore.getEntry("owlstead", param);
    if (!(entry instanceof PrivateKeyEntry)) {
      throw new KeyStoreException("That's not a private key!");
    }
    PrivateKeyEntry privKeyEntry = (PrivateKeyEntry) entry;
    PublicKey publicKey = privKeyEntry.getCertificate().getPublicKey();
    PrivateKey privateKey = privKeyEntry.getPrivateKey();
    return new KeyPair(publicKey, privateKey);
  }

  private static void storeToPKCS12(String filename, char[] password, KeyPair generatedKeyPair)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException,
      FileNotFoundException, OperatorCreationException, InvalidKeyException {

    Certificate selfSignedCertificate = selfSign(generatedKeyPair, "CN=owlstead");

    KeyStore pkcs12KeyStore = KeyStore.getInstance("PKCS12");
    pkcs12KeyStore.load(null, null);

    KeyStore.Entry entry = new PrivateKeyEntry(generatedKeyPair.getPrivate(),
        new Certificate[] { selfSignedCertificate });
    KeyStore.ProtectionParameter param = new KeyStore.PasswordProtection(password);

    pkcs12KeyStore.setEntry("owlstead", entry, param);

    try (FileOutputStream fos = new FileOutputStream(filename)) {
      pkcs12KeyStore.store(fos, password);
    }
  }
}
