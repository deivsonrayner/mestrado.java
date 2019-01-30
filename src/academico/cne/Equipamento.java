package academico.cne;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Equipamento {
	
	public Map<String, String> descricao = new HashMap<String, String>();
	public Map<String, String> tipoDescricao = new HashMap<String, String>();
	
	public Equipamento() throws IOException {
		InputStream is =  ClassLoader.getSystemResourceAsStream("equipamentos.csv");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line = null;
		boolean firstLine = true;
		while ((line = reader.readLine()) != null) {
			if (firstLine) {
				firstLine = false;
				continue;
			}
			String[] csvLine = line.split(",");
			
			descricao.put(csvLine[0], csvLine[1]);
			tipoDescricao.put(csvLine[2], csvLine[3]);
		}
		
	}
	
	public String codigo;
	public String tipo;
	public String cnes;
	public String uf;
	public int qtdExistente;
	public int qtdEmUso;
	public int indDispSUS;
	public int intNDispSUS;
	
	
	public String getDescricao() {
		return this.descricao.get(codigo);
	}
	
	public String getTipoDescricao() {
		return this.tipoDescricao.get(tipo);
	}
	
}
