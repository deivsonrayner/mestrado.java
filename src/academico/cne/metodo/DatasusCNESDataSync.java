package academico.cne.metodo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class DatasusCNESDataSync {
	
	
	public static void main(String[] args) throws InterruptedException {
		
		String FTP_URL = "ftp://ftp.datasus.gov.br/dissemin/publicos/CNES/200508_/Dados/";
		String entity  = "LT";
		String year    = "19";
		String saveTo  = "C:\\projetos\\mestrado\\dados\\cnes\\2019\\";
		
		
		
		String[] estados = new String[] {"AC","AL","AM","AP","BA","CE","DF", "ES", "GO","MA","MG","MS","MT","PA","PB", "PE","PI", "PR","RJ", "RN", "RO", "RR","RS","SC","SE","SP","TO"};
		//String[] estados = new String[] {"PI", "PR","RJ", "RN", "RO", "RR","RS","SC","SE","SP","TO"};
		
		for (int idx = 0; idx < estados.length; idx++) {
			for (int mes = 1; mes <= 12; mes++) {
				System.out.println("Processando  MES: "+mes+" ESTADO: "+estados[idx]);
				
				try {
					String fileName = entity+estados[idx]+year+String.format("%02d", mes)+".dbc";
					URL url = new URL(FTP_URL+entity+"/"+fileName);
					BufferedInputStream in = new BufferedInputStream(url.openStream());
					Path path = Paths.get(saveTo+fileName);
					Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
				
				} catch (IOException e) {
					mes--;
					System.out.println("ERRO: "+e.getMessage());
					Thread.sleep(2000);
				} 
			}
		}
		
	}

}
