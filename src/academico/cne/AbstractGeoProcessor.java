package academico.cne;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;

import com.jillesvangurp.geo.GeoGeometry;

public class AbstractGeoProcessor {

	public static Map<String, Setor> setoresCache = new HashMap<String, Setor>();
	public static Collection<GeoObjeto> geoObjetos = new ArrayList<GeoObjeto>();
	public static Collection<Estabelecimento> estabelecimentos = new ArrayList<Estabelecimento>();

	public static void carregarSetores(String dirOrigem, String regiao) throws IOException {
		File dir = new File(dirOrigem);
		
		for (File subDir : dir.listFiles()) {
			
			System.out.println("Processando Diretorio: " + subDir.getName());
			System.out.println("=================================================================");
			for (File file : subDir.listFiles()) {
				System.out.println("Avaliando Arquivo: " + file.getName());
				
				FileInputStream fileInput = new FileInputStream(file);
				byte[] content = new byte[(int) file.length()];
				fileInput.read(content);
				
				JSONArray json = new JSONArray(new String(content));
				
				for (int jIdx = 0; jIdx < json.length(); jIdx++) {
					if (!getIBGEAmostra(Integer.parseInt(json.getJSONObject(jIdx).getJSONObject("properties").getString("setor").substring(0, 6)),regiao)) {
						System.out.println("FORA DA AMOSTRA -->  IBGE: "+json.getJSONObject(jIdx).getJSONObject("properties").getString("setor").substring(0, 6));
						continue;
					}
					
					JSONArray coordenadas = json.getJSONObject(jIdx).getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0);
					Setor setor = new Setor();
					
					double[] ponto = PolygonUtilities.centerOfMass(PolygonUtilities.converteCoordenadasToPoint(coordenadas));
					setor.centroMassLng = ponto[0];
					setor.centroMassLat = ponto[1];
					
					double area = GeoGeometry.area(PolygonUtilities.converteCoordenadasToArray(coordenadas));
					setor.area = area;
					
					setor.regiao = subDir.getName();
					setor.ibge = json.getJSONObject(jIdx).getJSONObject("properties").getString("setor").substring(0, 7);
					setor.ibge6 = json.getJSONObject(jIdx).getJSONObject("properties").getString("setor").substring(0, 6);
					setor.id =  json.getJSONObject(jIdx).getJSONObject("properties").getString("setor");
					
					
					setoresCache.put(setor.id, setor);
					
				}
					
			}
			
		}
	}

	public static void carregarObjetos(String dirObjetos) throws IOException {
			File file = new File(dirObjetos);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			boolean firstLine = true;
			while ((line = reader.readLine()) != null) {
				if (firstLine) {
					firstLine = !firstLine;
					continue;
				}
				
				String[] lineSplit = line.split(",");
				if (lineSplit[1].equalsIgnoreCase("NA")) {
					continue;
				}
				GeoObjeto geo = new GeoObjeto();
				geo.id = lineSplit[0];
				geo.lng = Double.parseDouble(lineSplit[1]);
				geo.lat = Double.parseDouble(lineSplit[2]);
				geoObjetos.add(geo);
			}
			
		
	}

	public static void carregarEstabelecimentos(String dirObjetos) throws IOException {
			File file = new File(dirObjetos);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			boolean firstLine = true;
			while ((line = reader.readLine()) != null) {
				if (firstLine) {
					firstLine = !firstLine;
					continue;
				}
				
				String[] lineSplit = line.split(",");
				if (lineSplit[1].equalsIgnoreCase("NA")) {
					continue;
				}
				Estabelecimento estabelecimento = new Estabelecimento();
				estabelecimento.tipUnidade = lineSplit[0].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[0]);
				estabelecimento.cnes = lineSplit[1];
				estabelecimento.id = lineSplit[1];
				estabelecimento.nivDependencia = lineSplit[2].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[2]);
				estabelecimento.vincSus = lineSplit[3].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[3]);
				estabelecimento.tpGestao = lineSplit[4];
				estabelecimento.atividade = lineSplit[5].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[5]);
				estabelecimento.clientel = lineSplit[6].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[6]);
				estabelecimento.turnoAt = lineSplit[7].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[7]);
				estabelecimento.atendAmbulatorial = lineSplit[8].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[8]);
				estabelecimento.centroNeoNatal = lineSplit[9].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[9]);
				estabelecimento.atendHospitalar = lineSplit[10].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[10]);
				estabelecimento.urgEmergencia = lineSplit[11].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[11]);
				estabelecimento.centroCirurgico = lineSplit[12].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[12]);
				estabelecimento.centroObstetrico = lineSplit[13].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[13]);
				estabelecimento.nivelAtendAmb = lineSplit[14].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[14]);
				estabelecimento.nivelAtendHos = lineSplit[15].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[15]);
				estabelecimento.atendPr = lineSplit[16].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[16]);
				estabelecimento.longitude = lineSplit[21].equalsIgnoreCase("NA")?-1:Double.parseDouble(lineSplit[21]);
				estabelecimento.latitude = lineSplit[22].equalsIgnoreCase("NA")?-1:Double.parseDouble(lineSplit[22]);
				estabelecimento.ano = lineSplit[23].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[23]);
				estabelecimento.totalEquipes = lineSplit[24].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[24]);
				estabelecimento.atencaoBasica = lineSplit[25].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[25]);
				estabelecimento.mediaComplexidade = lineSplit[27].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[27]);
				estabelecimento.altaComplexidade = lineSplit[28].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[28]);
		
				estabelecimentos.add(estabelecimento);
			}
			
		
	}

	public static boolean getIBGEAmostra(int ibge, String regiao) {
		int[] amostra = null;
		
		if (regiao == null) {
			amostra = new int[] {231000,230350,230370,230395,230440,230765,230770,230960,230970,231085,230428,230495,230523,230625,231240,
					310930,310945,317040,520010,520017,520025, 520030, 520400, 520549, 520551, 520580, 520620, 520800, 521250, 521305, 521523, 521560, 521730, 521760, 521975, 522185, 522220, 530010,
					410020,410030,410040,410180,410230,410310,410400,410410,410420,410425,410520,410580,410620,410690,412863,410765,411125,411320,411430,411915,411910,411950,412080,412220,412120,412230,412550,412760,412788};
		}
		
		if (regiao != null && regiao.equalsIgnoreCase("CE")) {
			amostra = new int[] {231000,230350,230370,230395,230440,230765,230770,230960,230970,231085,230428,230495,230523,230625,231240};
		}
		
		if (regiao != null && regiao.equalsIgnoreCase("DF")) {
			//amostra = new int[] {310930,310945,317040,520010,520017,520025, 520030, 520400, 520549, 520551, 520580, 520620, 520800, 521250, 521305, 521523, 521560, 521730, 521760, 521975, 522185, 522220, 530010};
			amostra = new int[] {530010};

		}
		
		if (regiao != null && regiao.equalsIgnoreCase("PR")) {
			amostra = new int[] {410020,410030,410040,410180,410230,410310,410400,410410,410420,410425,410520,410580,410620,410690,412863,410765,411125,411320,411430,411915,411910,411950,412080,412220,412120,412230,412550,412760,412788};
		}
		 
		for (int item : amostra) {
			if (item == ibge) {
				return true;
			}
		}
	
		return false;
	}

	public AbstractGeoProcessor() {
		super();
	}

}