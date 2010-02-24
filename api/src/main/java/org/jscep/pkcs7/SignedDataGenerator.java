/*
 * Copyright (c) 2009-2010 David Grant
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jscep.pkcs7;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DEREncodableVector;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.cms.SignerIdentifier;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.X509Name;
import org.jscep.pkcs9.ContentTypeAttribute;
import org.jscep.pkcs9.MessageDigestAttribute;
import org.jscep.util.AlgorithmDictionary;
import org.jscep.util.LoggingUtil;


/**
 * This class is used for generating degenerate (certificates and CRLs only) 
 * {@link org.bouncycastle.asn1.cms.SignedData} instances.
 * <p>
 * Example usage:
 * <pre>
 * X509Certificate cert = ...;
 * 
 * SignedDataGenerator gen = new SignedDataGenerator();
 * gen.addCertificate(cert);
 * SignedData signedData = gen.generate();
 * </pre>
 * 
 * @author David Grant
 */
public class SignedDataGenerator {
	private static Logger LOGGER = LoggingUtil.getLogger("com.google.code.jscep.pkcs7");
	private final List<X509Certificate> certs;
	private final List<X509CRL> crls;
	private final List<AlgorithmIdentifier> digestAlgorithms;
	private final String digestAlgorithm = "MD5";
	private final Set<SignerInformation> signerInfos;
	
	/**
	 * Creates a new instance of <code>SignedDataGenerator</code>
	 */
	public SignedDataGenerator() {
		certs = new LinkedList<X509Certificate>();
		crls = new LinkedList<X509CRL>();
		digestAlgorithms = new LinkedList<AlgorithmIdentifier>();
		signerInfos = new HashSet<SignerInformation>();
	}
	
	/**
	 * Adds the provided certificate to the resulting {@link org.bouncycastle.asn1.cms.SignedData}.
	 * 
	 * @param cert the certificate to add.
	 */
	public void addCertificate(X509Certificate cert) {
		certs.add(cert);
	}
	
	public void addSigner(PrivateKey key, X509Certificate cert, String digestAlgorithm, String encryptionAlgorithm) {
		
	}
	
	public void addSigner(PrivateKey key, Collection<X509Certificate> certs, Collection<X509CRL> crls, String digestAlgorithm, String encryptionAlgorithm) {
		this.certs.addAll(certs);
		this.crls.addAll(crls);
		this.digestAlgorithms.add(AlgorithmDictionary.getAlgId(digestAlgorithm));
	}
	
	/**
	 * Adds the provided CRL to the resulting {@link org.bouncycastle.asn1.cms.SignedData}.
	 * 
	 * @param crl the CRL to add.
	 */
	public void addCRL(X509CRL crl) {
		crls.add(crl);
	}
	
	/**
	 * Generates a new {@link org.bouncycastle.asn1.cms.SignedData} instance.
	 * 
	 * @return a new <code>SignedData</code> instance.
	 * @throws IOException if any I/O error occurs.
	 * @throws NoSuchAlgorithmException 
	 */
	public SignedData generate() throws IOException {
		LOGGER.entering(getClass().getName(), "generate");
		
		final DERSet digestAlgorithms = getDigestAlgorithmIdentifiers();
		final ContentInfo contentInfo = getContentInfo();
		DERSet signerInfos;
		try {
			signerInfos = getSignerInfos(contentInfo);
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
		final DERSet certificates = getCertificates();
		final DERSet crls = getCRLs();
		
		final SignedData sd = new SignedData(digestAlgorithms, contentInfo, certificates, crls, signerInfos);
		
		LOGGER.exiting(getClass().getName(), "generate", sd);
		return sd;
	}
	
	private ContentInfo getContentInfo() {
		return new ContentInfo(getContentType(), getContent());
	}
	
	private DEREncodable getContent() {
		return new DEROctetString(new byte[0]);
	}
	
	private DERObjectIdentifier getContentType() {
		return CMSObjectIdentifiers.data;
	}
	
	private DERSet getDigestAlgorithmIdentifiers() {
		return new DERSet(getDigestAlgorithmIdentifiersVector());
	}
	
	private DEREncodableVector getDigestAlgorithmIdentifiersVector() {
		final DEREncodableVector v = new ASN1EncodableVector();
		
		for (SignerInformation signerInformation : signerInfos) {
			v.add(signerInformation.getDigestAlgorithm());
		}
		
		return v;
	}
	
	private DERSet getSignerInfos(ContentInfo contentInfo) throws NoSuchAlgorithmException, IOException {
		return new DERSet(getSignerInfoVector(contentInfo));
	}
	
	private DEREncodableVector getSignerInfoVector(ContentInfo contentInfo) throws NoSuchAlgorithmException, IOException {
		final DEREncodableVector v = new ASN1EncodableVector();
		
		for (SignerInformation signerInformation : signerInfos) {
			v.add(signerInformation.asSignerInfo(contentInfo));
		}
		
		return v;
	}
	
	private DERSet getCertificates() throws IOException {
		return new DERSet(getCertificatesVector());
	}
	
	private DEREncodableVector getCertificatesVector() throws IOException {
		final DEREncodableVector v = new ASN1EncodableVector();
		
		for (Certificate cert : certs) {
			try {
				v.add(ASN1Object.fromByteArray(cert.getEncoded()));
			} catch (CertificateEncodingException e) {
				// This is thrown if an encoding error occurs.
				throw new IOException(e);
			}
		}
		
		return v;
	}
	
	private DERSet getCRLs() throws IOException {
		return new DERSet(getCRLsVector());
	}
	
	private DEREncodableVector getCRLsVector() throws IOException {
		final DEREncodableVector v = new ASN1EncodableVector();
		
		for (CRL crl : crls) {
			final X509CRL x509crl = (X509CRL) crl;
			try {
				v.add(ASN1Object.fromByteArray(x509crl.getEncoded()));
			} catch (CRLException e) {
				// This is thrown if an encoding error occurs.
				throw new IOException(e);
			}
		}
		
		return v;
	}
	
	private class SignerInformation {		
		private final IssuerAndSerialNumber iasn;
		private final AlgorithmIdentifier digestAlgorithm;
		private final Set<Attribute> authenticatedAttributes;
		private final AlgorithmIdentifier digestEncryptionAlgorithm;
		private final Set<Attribute> unauthenticatedAttributes;
		
		public SignerInformation(IssuerAndSerialNumber iasn, AlgorithmIdentifier digestAlgorithm, AlgorithmIdentifier digestEncryptionAlgorithm) {
			this.iasn = iasn;
			this.digestAlgorithm = digestAlgorithm;
			this.authenticatedAttributes = null;
			this.digestEncryptionAlgorithm = digestEncryptionAlgorithm;
			this.unauthenticatedAttributes = null;
		}
		
		public AlgorithmIdentifier getDigestAlgorithm() {
			return digestAlgorithm;
		}
		
		private SignerIdentifier getSignerIdentifier() {
			return new SignerIdentifier(iasn);
		}
		
		private DERSet getAuthenticatedAttributes(ContentInfo contentInfo) throws NoSuchAlgorithmException, IOException {
			// The field is optional, but it must be present if the content
            // type of the ContentInfo value being signed is not data.
			if (contentInfo.getContentType().equals(PKCSObjectIdentifiers.data) == false) {
				// Optional
			} else {
				// If the field is present, it must contain, at a minimum, two
				// attributes:
				
				// A PKCS #9 content-type attribute having as its value the 
				// content type of the ContentInfo value being signed.
				Attribute contentType = new ContentTypeAttribute(contentInfo.getContentType());
				// A PKCS #9 message-digest attribute, having as its value 
				// the message digest of the content (see below).
				Attribute messageDigest = new MessageDigestAttribute(getMessageDigest(contentInfo));
			}
			// TODO
			return new DERSet();
		}
		
		private ASN1OctetString getEncryptedDigest(ContentInfo contentInfo) throws NoSuchAlgorithmException, IOException {
			// When the [authenticatedAttributes] field is absent, the result 
			// is just the message digest of the content.
			byte[] digest = getMessageDigest(contentInfo);
			// When the [authenticatedAttributes] field is present, however, 
			// the result is the message digest of the complete DER encoding 
			// of the Attributes value containted in the authenticatedAttributes 
			// field.
			
			// TODO
			return new DEROctetString(new byte[0]);
		}
		
		private byte[] getMessageDigest(ContentInfo contentInfo) throws NoSuchAlgorithmException, IOException {
			final String digestAlgName = AlgorithmDictionary.lookup(digestAlgorithm);
			final MessageDigest digest = MessageDigest.getInstance(digestAlgName);
			
			// TODO
			
			// the initial input to the message-digesting process is the "value" 
			// of the content being signed.  Specifically, the initial input is 
			// the contents octets of the DER encoding of the content field of 
			// the ContentInfo value to which the signing process is applied.
			//
			// return digest.digest(contentInfo.getContent().getDERObject().getDEREncoded());
			return digest.digest(contentInfo.getEncoded());
		}
		
		private DERSet getUnauthenticatedAttributes() {
			// TODO
			return new DERSet();
		}
		
		public SignerInfo asSignerInfo(ContentInfo contentInfo) throws NoSuchAlgorithmException, IOException {
			SignerInfo info = new SignerInfo(getSignerIdentifier(), 
											 digestAlgorithm, 
											 getAuthenticatedAttributes(contentInfo),
											 digestEncryptionAlgorithm,
											 getEncryptedDigest(contentInfo),
											 getUnauthenticatedAttributes());
			return info;
		}
	}
}