package academico.cne;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class PreparacaoST_Geolocalizacao {

	public static void main(String[] args) throws IOException, UnsupportedOperationException, SAXException,
			ParserConfigurationException, InterruptedException, XPathExpressionException {

		long start = System.currentTimeMillis();
		String cne_principal = args[0];
		String target = args[1];
		String line = null;
		String splitBy = ",";
		int skip = 0;
		int stop = -1;

		BufferedReader pReader = new BufferedReader(new FileReader(cne_principal));
		BufferedWriter tWriter = new BufferedWriter(new FileWriter(target));

		boolean firstLine = true;

		String[] geoFirstLine = null;
		HttpClient client = HttpClientBuilder.create().disableAuthCaching().disableCookieManagement()
				.evictExpiredConnections().build();

		int count = 0;

		String header = "LINHA,CNES,CODUFMUN,COD_CEP,CPF_CNPJ,PF_PJ,NIV_DEP,CNPJ_MAN,COD_IR,REGSAUDE,MICR_REG,DISTRSAN,DISTRADM,VINC_SUS,TPGESTAO,ESFERA_A,RETENCAO,ATIVIDAD,NATUREZA,CLIENTEL,TP_UNID,TURNO_AT,NIV_HIER,TP_PREST,CO_BANCO,CO_AGENC,C_CORREN,CONTRATM,DT_PUBLM,CONTRATE,DT_PUBLE,ALVARA,DT_EXPED,ORGEXPED,AV_ACRED,CLASAVAL,DT_ACRED,AV_PNASS,DT_PNASS,GESPRG1E,GESPRG1M,GESPRG2E,GESPRG2M,GESPRG4E,GESPRG4M,NIVATE_A,GESPRG3E,GESPRG3M,GESPRG5E,GESPRG5M,GESPRG6E,GESPRG6M,NIVATE_H,QTLEITP1,QTLEITP2,QTLEITP3,LEITHOSP,QTINST01,QTINST02,QTINST03,QTINST04,QTINST05,QTINST06,QTINST07,QTINST08,QTINST09,QTINST10,QTINST11,QTINST12,QTINST13,QTINST14,URGEMERG,QTINST15,QTINST16,QTINST17,QTINST18,QTINST19,QTINST20,QTINST21,QTINST22,QTINST23,QTINST24,QTINST25,QTINST26,QTINST27,QTINST28,QTINST29,QTINST30,ATENDAMB,QTINST31,QTINST32,QTINST33,CENTRCIR,QTINST34,QTINST35,QTINST36,QTINST37,CENTROBS,QTLEIT05,QTLEIT06,QTLEIT07,QTLEIT08,QTLEIT09,QTLEIT19,QTLEIT20,QTLEIT21,QTLEIT22,QTLEIT23,QTLEIT32,QTLEIT34,QTLEIT38,QTLEIT39,QTLEIT40,CENTRNEO,ATENDHOS,SERAP01P,SERAP01T,SERAP02P,SERAP02T,SERAP03P,SERAP03T,SERAP04P,SERAP04T,SERAP05P,SERAP05T,SERAP06P,SERAP06T,SERAP07P,SERAP07T,SERAP08P,SERAP08T,SERAP09P,SERAP09T,SERAP10P,SERAP10T,SERAP11P,SERAP11T,SERAPOIO,RES_BIOL,RES_QUIM,RES_RADI,RES_COMU,COLETRES,COMISS01,COMISS02,COMISS03,COMISS04,COMISS05,COMISS06,COMISS07,COMISS08,COMISS09,COMISS10,COMISS11,COMISS12,COMISSAO,AP01CV01,AP01CV02,AP01CV05,AP01CV06,AP01CV03,AP01CV04,AP02CV01,AP02CV02,AP02CV05,AP02CV06,AP02CV03,AP02CV04,AP03CV01,AP03CV02,AP03CV05,AP03CV06,AP03CV03,AP03CV04,AP04CV01,AP04CV02,AP04CV05,AP04CV06,AP04CV03,AP04CV04,AP05CV01,AP05CV02,AP05CV05,AP05CV06,AP05CV03,AP05CV04,AP06CV01,AP06CV02,AP06CV05,AP06CV06,AP06CV03,AP06CV04,AP07CV01,AP07CV02,AP07CV05,AP07CV06,AP07CV03,AP07CV04,ATEND_PR,DT_ATUAL,COMPETEN,NAT_JUR,X_UF,X_ANO,X_MES,X_NOME_FANTASIA,X_NOME_EMPRESARIAL,X_END_LOGRAD,X_END_NUM,X_END_CEP,X_END_BAIRRO,X_LONGITUDE,X_LATITUDE,X_IBGE";
		tWriter.write(header);
		tWriter.newLine();
		tWriter.flush();

		while ((line = pReader.readLine()) != null) {
			if (firstLine && skip == 0) {
				firstLine = false;
				count++;
				continue;
			}
			if (count <= skip) {
				System.out.println("SKIP " + count + " |------| SKIPPING THIS LINE");
				count++;
				continue;
			}
			if (count == stop) {
				System.out.println("BREAK " + count + " |------| BREAKING ON THIS LINE");
				break;
			}

			if (count % 500 == 0) {
				client = HttpClientBuilder.create().disableAuthCaching().disableCookieManagement()
						.evictExpiredConnections().build();
				System.out.println("*** RENEWING CONNECTION ----");
			}

			String[] cneLine = line.split(splitBy);

			String newLineStr = "";

			String nomeFantasia = null;
			String nomeEmpresarial = null;
			String enderecoLogradouro = null;
			String enderecoNumero = null;
			String enderecoCep = null;
			String enderecoBairro = null;
			String latitude = null;
			String longitude = null;
			String ibge = null;
			String cnes = cneLine[1];

			HttpResponse response = null;
			try {
				response = pesquisarDatasus(client, cnes);
				if (response.getStatusLine().getStatusCode() != 200) {
					System.out.println(
							"TRYING -- CNES:" + cneLine[1] + " |------| " + response.getStatusLine().getReasonPhrase());
					Thread.currentThread().sleep(5000);
					response = pesquisarDatasus(client, cnes);
					if (response.getStatusLine().getStatusCode() == 500) {
						System.out.println("IGNORIG -- CNES:" + cneLine[1] + " |------| "
								+ response.getStatusLine().getReasonPhrase());
					}
				}

			} catch (Throwable t) {
				System.out.println("TRYING -- CNES:" + cneLine[1] + " |------| " + t.getMessage());
				Thread.currentThread().sleep(1000);
				response = pesquisarDatasus(client, cnes);
			}

			if (response.getStatusLine().getStatusCode() == 200) {

				ByteArrayOutputStream output = new ByteArrayOutputStream();
				response.getEntity().writeTo(output);
				String out = output.toString();
				InputSource xmlInput = new InputSource(new StringReader(out));

				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder;
				Document document = null;
				try {
					builder = factory.newDocumentBuilder();
					document = builder.parse(xmlInput);
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException();
				}

				XPathFactory xpathFactory = XPathFactory.newInstance();
				XPath xpath = xpathFactory.newXPath();

				nomeFantasia = getValueFromXML("nomeFantasia", xpath, document, 0);
				nomeEmpresarial = getValueFromXML("nomeEmpresarial", xpath, document, 0);
				ibge = getValueFromXML("Municipio", xpath, document, 0);

				NodeList enderecoNode = ((NodeList) xpath.compile("//*[local-name()='Endereco']").evaluate(document,
						XPathConstants.NODESET));
				for (int idx = 0; idx < enderecoNode.item(0).getChildNodes().getLength(); idx++) {
					Node node = enderecoNode.item(0).getChildNodes().item(idx);

					if (node.getNodeName().contains("nomeLogradouro")) {
						enderecoLogradouro = node.getTextContent();
					} else if (node.getNodeName().contains("numero")) {
						enderecoNumero = node.getTextContent();
					} else if (node.getNodeName().contains("Bairro")) {
						if (node.getChildNodes().item(0) != null)
							enderecoBairro = node.getChildNodes().item(0).getTextContent();
					} else if (node.getNodeName().contains("CEP")) {
						enderecoCep = node.getChildNodes().item(0).getTextContent();
					}
				}

				String tipoUnidade = getValueFromXML("tipoUnidade", xpath, document, 0);
				String tipoUnidadeDes = getValueFromXML("tipoUnidade", xpath, document, 1);

				Node localizacao = ((NodeList) xpath.compile("//*[local-name()='Localizacao']").evaluate(document,
						XPathConstants.NODESET)).item(0);
				if (localizacao != null) {

					longitude = localizacao.getChildNodes().item(0).getTextContent();
					latitude = localizacao.getChildNodes().item(1).getTextContent();
				}
			} else {
				System.out.println(
						"ERROR -- CNES:" + cneLine[1] + " |------| " + response.getStatusLine().getReasonPhrase());
				client = HttpClientBuilder.create().disableAuthCaching().disableCookieManagement()
						.evictExpiredConnections().build();
			}

			nomeFantasia = nomeFantasia == null || nomeFantasia.trim().isEmpty() ? "NA" : nomeFantasia;
			nomeEmpresarial = nomeEmpresarial == null || nomeEmpresarial.trim().isEmpty() ? "NA" : nomeEmpresarial;
			enderecoLogradouro = enderecoLogradouro == null || enderecoLogradouro.trim().isEmpty() ? "NA"
					: enderecoLogradouro;
			enderecoNumero = enderecoNumero == null || enderecoNumero.trim().isEmpty() ? "NA" : enderecoNumero;
			enderecoCep = enderecoCep == null || enderecoCep.trim().isEmpty() ? "NA" : enderecoCep;
			enderecoBairro = enderecoBairro == null || enderecoBairro.trim().isEmpty() ? "NA" : enderecoBairro;
			latitude = latitude == null || latitude.trim().isEmpty() ? "NA" : latitude;
			longitude = longitude == null || longitude.trim().isEmpty() ? "NA" : longitude;
			ibge = ibge == null || ibge.trim().isEmpty() ? "NA" : ibge;

			int idx = 0;
			for (String item : cneLine) {
				if (newLineStr.isEmpty()) {
					newLineStr = newLineStr + item;
				} else {
					newLineStr = newLineStr + "," + item;
				}
				idx++;
			}

			newLineStr = newLineStr + "," + nomeFantasia + "," + nomeEmpresarial + "," + enderecoLogradouro + ","
					+ enderecoNumero + "," + enderecoCep + "," + enderecoBairro + "," + longitude + "," + latitude + ","
					+ ibge;

			System.out.println("LINE " + count + " |------| " + newLineStr);
			tWriter.write(newLineStr);
			tWriter.flush();
			tWriter.newLine();
			tWriter.flush();
			count++;
		}

		tWriter.flush();
		tWriter.close();
		long totalTime = System.currentTimeMillis() - start;

		System.out.println("TOTAL TIME (s): " + totalTime / 1000);

	}

	private static HttpResponse pesquisarDatasus(HttpClient client, String cnes)
			throws UnsupportedEncodingException, InterruptedException, IOException, ClientProtocolException {
		String datasusServiceRequest = "<soap:Envelope " + "xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" "
				+ "xmlns:est=\"http://servicos.saude.gov.br/cnes/v1r0/estabelecimentosaudeservice\" "
				+ "xmlns:fil=\"http://servicos.saude.gov.br/wsdl/mensageria/v1r0/filtropesquisaestabelecimentosaude\" "
				+ "xmlns:cod=\"http://servicos.saude.gov.br/schema/cnes/v1r0/codigocnes\" "
				+ "xmlns:cnpj=\"http://servicos.saude.gov.br/schema/corporativo/pessoajuridica/v1r0/cnpj\">"
				+ "<soap:Header>" + "<wsse:Security soap:mustUnderstand=\"true\" "
				+ "xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" "
				+ "xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">"
				+ "<wsse:UsernameToken wsu:Id=\"UsernameToken-5FCA58BED9F27C406E14576381084652\">"
				+ "<wsse:Username>CNES.PUBLICO</wsse:Username>"
				+ "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">cnes#2015public</wsse:Password>"
				+ "</wsse:UsernameToken>" + "</wsse:Security>" + "</soap:Header>" + "<soap:Body>"
				+ "<est:requestConsultarEstabelecimentoSaude>" + "<fil:FiltroPesquisaEstabelecimentoSaude>"
				+ "<cod:CodigoCNES>" + "<cod:codigo>" + cnes + "</cod:codigo>" + "</cod:CodigoCNES>"
				+ "</fil:FiltroPesquisaEstabelecimentoSaude>" + "</est:requestConsultarEstabelecimentoSaude>"
				+ "</soap:Body>" + "</soap:Envelope>";

		HttpPost post = new HttpPost("https://servicos.saude.gov.br/cnes/EstabelecimentoSaudeService/v1r0");
		StringEntity entity = new StringEntity(datasusServiceRequest, "text/xml", "UTF-8");
		post.setEntity(entity);

		HttpResponse response = client.execute(post);
		return response;
	}

	public static String getValueFromXML(String nodeName, XPath xpath, Document document, int childIndx) {
		String value = null;
		try {
			value = ((NodeList) xpath.compile("//*[local-name()='" + nodeName + "']").evaluate(document,
					XPathConstants.NODESET)).item(0).getChildNodes().item(childIndx).getTextContent();
		} catch (Throwable t) {
		}
		return value;

	}

}
