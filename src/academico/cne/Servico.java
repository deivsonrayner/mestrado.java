package academico.cne;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Servico {
	
	public Map<String, String> tipoServico = new HashMap<String, String>();
	public Map<String, String> tipoClasse = new HashMap<String, String>();
	
	public Servico() throws IOException {
		InputStream is =  ClassLoader.getSystemResourceAsStream("servicos-especializados-tipos.csv");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line = null;
		boolean firstLine = true;
		while ((line = reader.readLine()) != null) {
			if (firstLine) {
				firstLine = false;
				continue;
			}
			String[] csvLine = line.split(",");
			
			tipoServico.put(csvLine[0], csvLine[1]);
		}
		
		is =  ClassLoader.getSystemResourceAsStream("servicos-especializados-classe.csv");
		reader = new BufferedReader(new InputStreamReader(is));
		line = null;
		firstLine = true;
		while ((line = reader.readLine()) != null) {
			if (firstLine) {
				firstLine = false;
				continue;
			}
			String[] csvLine = line.split(",");
			
			tipoClasse.put(csvLine[1]+"."+csvLine[0], csvLine[2]);
		}
		
	}
	
	public String codigo;
	public String classe;
	public String cnes;
	public String uf;
	public int ambSUS;
	public int ambNSUS;
	public int hospSUS;
	public int hospNSUS;
	
	
	public String getDescricao() {
		return this.tipoServico.get(codigo);
	}
	
	public String getTipoDescricao() {
		return this.tipoClasse.get(codigo + "." + classe);
	}

}
