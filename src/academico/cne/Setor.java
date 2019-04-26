package academico.cne;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Setor {
	public String id = null;
	public String regiao = null;
	public double centroMassLng = 0;
	public double centroMassLat = 0;
	public String ibge = null;
	public String ibge6=null;
	public double area = 0;
	public Point2D[] coordenadas = null;
	public String geoJson = null;
	public int camada = 0;
	
	public Map<String, String> municipios = new HashMap<String, String>();
	
	public Setor() throws IOException {
		InputStream is =  ClassLoader.getSystemResourceAsStream("municipios.csv");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line = null;
		boolean firstLine = true;
		while ((line = reader.readLine()) != null) {
			if (firstLine) {
				firstLine = false;
				continue;
			}
			String[] csvLine = line.split(",");
			
			municipios.put(csvLine[0], csvLine[1]);
		}
	}
	
	public String getMunicipio() {
		return this.municipios.get(this.ibge6);
	}
	
    
}