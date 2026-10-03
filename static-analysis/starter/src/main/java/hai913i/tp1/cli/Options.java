package hai913i.tp1.cli;

import java.nio.file.Path;

/** Validated command-line options. Reading arguments only: no analysis here. */
public record Options(Path project, Action action, Integer x, String method, String astFile) {

  public static final String USAGE = String.join("\n",
          "Usage : java -jar hai913i-tp1-analyzer.jar PROJET [options]",
          "  PROJET            dossier du projet a analyser (contenant src/ ou src/main/java)",
          "  --action ACTION   structure | calls | metrics | graph | ast | all   (defaut : all)",
          "  --x N             seuil X de la question 11, entier >= 0 ; obligatoire pour metrics et all",
          "  --method TEXTE    avec graph : appelees et appelantes d'une methode (id complet ou partiel)",
          "  --file NOM        avec ast : fichier a afficher (defaut : Dvd.java et Category.java)",
          "Exemple : java -jar target/hai913i-tp1-analyzer.jar ../resources/validation --x 5");

  public static Options parse(String[] args) throws UsageException {
    if (args.length == 0) throw new UsageException("chemin du projet manquant");

    Path project = Path.of(args[0]);
    Action action = Action.ALL;
    Integer x = null;
    String method = null;
    String file = null;

    // Options come in pairs: --name value
    for (int i = 1; i < args.length; i++) {
      String option = args[i];
      if (i + 1 >= args.length) throw new UsageException("valeur manquante apres " + option);
      String value = args[++i];
      switch (option) {
        case "--action" -> action = Action.parse(value);
        case "--x"      -> x = parseX(value);
        case "--method" -> method = value;
        case "--file"   -> file = value;
        default         -> throw new UsageException("option inconnue : " + option);
      }
    }

    if (action.needsX() && x == null) {
      throw new UsageException("le seuil X est obligatoire pour l'action "
              + action.name().toLowerCase() + " (--x N)");
    }
    return new Options(project, action, x, method, file);
  }

  private static int parseX(String value) throws UsageException {
    try {
      int x = Integer.parseInt(value);
      if (x >= 0) return x;
    } catch (NumberFormatException e) {
      // handled below
    }
    throw new UsageException("le seuil X doit etre un entier positif ou nul (recu : " + value + ")");
  }
}