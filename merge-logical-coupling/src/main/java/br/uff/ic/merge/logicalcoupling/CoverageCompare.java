package br.uff.ic.merge.logicalcoupling;

import domain.Dominoes;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jdt.core.dom.IMethodBinding;

import RepositoryImporter.CommitNode;
import RepositoryImporter.RepositoryNode;
import br.uff.ic.coupling.ChunkInformation;
import br.uff.ic.gems.resources.operation.Operation;
import br.uff.ic.mergeguider.MergeGuider;
import br.uff.ic.mergeguider.javaparser.ClassLanguageContructs;
import br.uff.ic.mergeguider.javaparser.ProjectAST;
import br.uff.ic.mergeguider.languageConstructs.Location;
import br.uff.ic.mergeguider.languageConstructs.MyMethodDeclaration;

/**

 * @author Cristiane, jjcfigueiredo, 
 */
public class CoverageCompare {

	public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException, ParseException {
		/*String input = "C:\\Users\\Carlos\\clones";
		String outputPathName = "C:\\Users\\Carlos\\logical_result";
		Double threshold = 0.3;*/

		String input = "";
		String outputPathName = "";
		Double threshold = 0.0;

		try {

			final Options options = new Options();

			options.addOption("i", true, "input directory");
			options.addOption("o", true, "output directory");
			options.addOption("t", true, "threshold from 0.0 to 1.0");

			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "merge-logical-coupling", options, true);

			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse( options, args);

			if(cmd.hasOption("i")) {
				input  = cmd.getOptionValue("i");
			}
			if (cmd.hasOption("o")){
				outputPathName  = cmd.getOptionValue("o");
			}
			if (cmd.hasOption("t")){
				threshold  = Double.parseDouble(cmd.getOptionValue("t"));
			}
		}catch (ParseException ex) {
			Logger.getLogger(CoverageCompare.class.getName()).log(Level.SEVERE, null, ex);

		}
		File directory = new File(input);

		// primeiro commit do projeto
		for (File project : directory.listFiles()) {
			if (project.isDirectory()) {

				new File(outputPathName + File.separator + "sandbox-" + project.getName()).mkdir();
				String sandbox = outputPathName + File.separator + "sandbox-" + project.getName();

				try (FileWriter file = new FileWriter(new File(outputPathName + File.separator + project.getName() + ".txt"))) {
					System.out.println("\nAnalyzing the project\t" + project.getName());

					String projectPath = project.toString();
					String SHALeft = "";
					String SHARight = "";
					String SHAmergeBase = "";

					String firstHash = Git.getFirstHash(projectPath);

					String _database = fillDatabase(project, project.getName(), outputPathName); //create database

					List<String> merges = Git.getMergeRevisions(projectPath);
					for (String SHAMerge : merges) {
						List<String> parents2 = getParents(projectPath, SHAMerge);
						if (parents2.size() == 2) {
							SHALeft = parents2.get(0);
							SHARight = parents2.get(1);
							SHAmergeBase = getMergeBase(projectPath, SHALeft, SHARight);
							// Check if is a fast-forward merge
							if ((!(SHAmergeBase.equals(SHALeft))) && (!(SHAmergeBase.equals(SHARight)))) {

								//Getting modified files 
								List<String> changedFilesLeft = new ArrayList<String>();
								List<String> changedFilesRight = new ArrayList<String>();

								List<String> changedFilesLeftAux = Git.getChangedFiles(projectPath, SHALeft, SHAmergeBase);
								List<String> changedFilesRightAux = Git.getChangedFiles(projectPath, SHARight, SHAmergeBase);

								//to remove files that have extension other than java
								for (int i = 0; i < changedFilesLeftAux.size(); i++) {
									if (changedFilesLeftAux.get(i).endsWith("java")) {
										changedFilesLeft.add(changedFilesLeftAux.get(i));
									}
								}
								for (int i = 0; i < changedFilesRightAux.size(); i++) {
									if (changedFilesRightAux.get(i).endsWith("java")) {
										changedFilesRight.add(changedFilesRightAux.get(i));
									}
								}
								//If not exist java files, the variable changedFiles can be empty and we can't identify dependencies
								if (!(changedFilesLeft.isEmpty()) || !(changedFilesRight.isEmpty())) {
									//Extracting Left AST
									System.out.println(SHAMerge);
									System.out.println("Cloning left repository...");
									String repositoryLeft = sandbox + File.separator + "left";

									MergeGuider.clone(projectPath, repositoryLeft);
									Git.reset(repositoryLeft);
									Git.clean(repositoryLeft);
									Git.checkout(repositoryLeft, SHALeft);

									System.out.println("Extracting left repository AST...");

									List<ClassLanguageContructs> ASTLeft = extractAST(repositoryLeft);

									//Extracting Right AST
									System.out.println("Cloning right repository...");

									String repositoryRight = sandbox + File.separator + "right";

									MergeGuider.clone(projectPath, repositoryRight);
									Git.reset(repositoryRight);
									Git.clean(repositoryRight);
									Git.checkout(repositoryRight, SHARight);

									System.out.println("Extracting right repository AST...");

									List<ClassLanguageContructs> ASTRight = extractAST(repositoryRight);

									//Extracting merge-base AST
									System.out.println("Cloning merge-base repository...");
									String repositoryBase = sandbox + File.separator + "base";

									MergeGuider.clone(projectPath, repositoryBase);
									Git.reset(repositoryBase);
									Git.clean(repositoryBase);
									Git.checkout(repositoryBase, SHAmergeBase);

									System.out.println("Extracting merge-base repository AST...");

									List<ClassLanguageContructs> ASTmergeBase = extractAST(repositoryBase);

									//Getting modified files AST
									List<ClassLanguageContructs> ASTchangedFilesLeft = generateASTFiles(ASTLeft, changedFilesLeft);
									List<ClassLanguageContructs> ASTchangedFilesRight = generateASTFiles(ASTRight, changedFilesRight);

									//Getting chunks tem que armazenar as linhas add e removidas para cada arquivo
									List<ChunkInformation> cisL = ChunkInformation.extractChunksInformation(repositoryLeft, changedFilesLeft, SHAmergeBase, SHALeft, "Left");
									List<ChunkInformation> cisR = ChunkInformation.extractChunksInformation(repositoryRight, changedFilesRight, SHAmergeBase, SHARight, "Right");

									List<EditedMethod> editedMethodLeft = generateEditedMethod(cisL, repositoryLeft, ASTchangedFilesLeft, ASTmergeBase);
									List<EditedMethod> editedMethodRight = generateEditedMethod(cisR, repositoryRight, ASTchangedFilesRight, ASTmergeBase);

									MergeMethods mergeMethods = getMergeMethods(project, SHAMerge, editedMethodLeft, editedMethodRight);

									/*List<Map<EditedMethod, Set<EditedMethod>>> dependencies = getMethodsDependencies(
											project, mergeMethods, firstHash, threshold, _database);*/
									List<Dependency_Information> dependencies = getMethodsDependencies(
											project, mergeMethods, firstHash, threshold, _database);

									boolean hasFilesDependencies = false;

									if (!(dependencies.get(0).getDependencyMap().isEmpty()) || !(dependencies.get(1).getDependencyMap().isEmpty())) {
										hasFilesDependencies = true;
									}

									if (hasFilesDependencies) {

										float intensityL = 0;
										float intensityR = 0;
										float total_coupling = 0;
										float couplingL = 0;
										float couplingR = 0;
										float normalized_coupling = 0;
										//float third = 0;

										if (!(dependencies.get(0).getDependencyMap().isEmpty())){
											intensityL = dependencies.get(0).getValues();
											couplingL =  dependencies.get(0).getCoupling();
										}

										if (!(dependencies.get(1).getDependencyMap().isEmpty())) {
											intensityR = dependencies.get(1).getValues();
											couplingR =  dependencies.get(1).getCoupling();
										}

										total_coupling = (intensityL + intensityR)/2;
										float chunks = editedMethodLeft.size() + editedMethodRight.size();
										float couplings  = couplingL + couplingR;

										if (chunks > 0)
										{
											normalized_coupling = total_coupling/chunks;
										}
										
										/*if (couplings > 0) {
											third = intensityMerge/couplings;
										}*/

										file.write(SHAMerge + ", "  + chunks + ", " + intensityL + ", " + intensityR + ", " + total_coupling + ", " + normalized_coupling + "\n");
									}
								} else {
									System.out.println(SHAMerge + "does not has java files in both branches");
								}
							}
						}

					} file.close();

				} catch (IOException ex) {
					Logger.getLogger(CoverageCompare.class.getName()).log(Level.SEVERE, null, ex);
					System.out.println("The file could not be created");
				}

			}//if project

			//
		}//for projects

	}//main
	private static MergeMethods getMergeMethods(File project, String mergeHash, 
			List<EditedMethod>editedMethodLeft, List<EditedMethod>editedMethodRight) {
		MergeMethodsDao mergeMethodsDao = new MergeMethodsDao();

		MergeMethods mergeSelected = mergeMethodsDao.getMerge(mergeHash, project);

		//EditedMethodsDao methodsDao = new EditedMethodsDao();

		mergeSelected.setMethodsOnBranchOne(editedMethodLeft);

		mergeSelected.setMethodsOnBranchTwo(editedMethodRight);

		return mergeSelected;
	}

	/*private static List<Map<EditedMethod, Set<EditedMethod>>> getMethodsDependencies(File project,
			MergeMethods mergeMethods, String firstHash, Double threshold, String _database) {*/

	private static List<Dependency_Information> getMethodsDependencies(File project,
			MergeMethods mergeMethods, String firstHash, Double threshold, String _database) {

		//List<Map<EditedMethod, Set<EditedMethod>>> depList = new ArrayList<>();

		List<Dependency_Information> depList = new ArrayList<>();

		//Dependency_Information depList = null;

		// MergeCommitsDao mCommitsDao = new MergeCommitsDao(project);
		List<String> hashsOnPreviousHistory = Git.getHashs(project.getPath(), firstHash, mergeMethods.getHashBase()); 
		// pega os commits anteriores até o base

		try {

			Set<String> editedMethods = new HashSet<>();
			mergeMethods.getMethodsOnBranchOne().stream().forEach((editedMethod) -> {
				editedMethods.add("'" + editedMethod.getMethodName() + "'");
			});
			mergeMethods.getMethodsOnBranchTwo().stream().forEach((editedMethod) -> {
				editedMethods.add("'" + editedMethod.getMethodName() + "'");
			});

			List<Integer> matrices = new ArrayList<>(Arrays.asList(2));// ALTERAR AKI PARA 2 OU 5
			System.out.println("Creating the dominoes of dependencies");
			ArrayList<Dominoes> dominoesHistory = DominoesFiles.loadMatrices(_database, project.getName(),
					"CPU", hashsOnPreviousHistory, editedMethods, matrices);
			Dominoes domCF = dominoesHistory.get(0);
			Dominoes domCFt = domCF.cloneNoMatrix();
			domCFt.transpose();

			// System.out.println("Matrix 1 and 2 non-zeros\t" +
			// domCF.getMat().getNonZeroData().size() + "\t" +
			// domCFt.getMat().getNonZeroData().size());
			Dominoes domFF = domCFt.multiply(domCF);
			domFF.confidence();

			Dependencies dependencies = new Dependencies(domFF);
			//double threshold = Parameter.THRESHOLD;

			depList.add(0, dependencies.getDependenciesAcrossBranches(mergeMethods.getMethodsOnBranchOne(),
					mergeMethods.getMethodsOnBranchTwo(), threshold));
			depList.add(1, dependencies.getDependenciesAcrossBranches(mergeMethods.getMethodsOnBranchTwo(),
					mergeMethods.getMethodsOnBranchOne(), threshold));
			/*
			depList.add(0, dependencies.getDependenciesAcrossBranches(mergeMethods.getMethodsOnBranchOne(),
			mergeMethods.getMethodsOnBranchTwo(), threshold));
			depList.add(1, dependencies.getDependenciesAcrossBranches(mergeMethods.getMethodsOnBranchTwo(),
			mergeMethods.getMethodsOnBranchOne(), threshold));*/


		} catch (SQLException ex) {
			Logger.getLogger(CoverageCompare.class.getName()).log(Level.SEVERE, null, ex);

		} catch (Exception ex) {
			Logger.getLogger(CoverageCompare.class.getName()).log(Level.SEVERE, null, ex);
		}

		return depList;
	}

	public static List<String> getParents(String repositoryPath, String revision) {
		// String command = "git rev-list --parents -n 1 " + revision;
		String command = "git log --pretty=%P -n 1 " + revision;

		List<String> output = new ArrayList<String>();

		try {
			Process exec = Runtime.getRuntime().exec(command, null, new File(repositoryPath));

			String s;

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(exec.getInputStream()));

			BufferedReader stdError = new BufferedReader(new InputStreamReader(exec.getErrorStream()));

			// read the output from the command
			while ((s = stdInput.readLine()) != null) {
				String[] split = s.split(" ");
				for (String rev : split) {
					output.add(rev);
				}

			}

			// read any errors from the attempted command
			while ((s = stdError.readLine()) != null) {
				System.out.println(s);

			}

		} catch (IOException ex) {
			Logger.getLogger(CoverageCompare.class.getName()).log(Level.SEVERE, null, ex);
		}

		return output;
	}

	public static String getMergeBase(String repositoryPath, String commit1, String commit2) {
		// String command = "git rev-list --parents -n 1 " + revision;
		String command = "git merge-base " + commit1 + " " + commit2;

		String output = null;

		try {
			Process exec = Runtime.getRuntime().exec(command, null, new File(repositoryPath));

			String s;

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(exec.getInputStream()));

			BufferedReader stdError = new BufferedReader(new InputStreamReader(exec.getErrorStream()));

			// read the output from the command
			while ((s = stdInput.readLine()) != null) {
				output = s;
			}

			// read any errors from the attempted command
			while ((s = stdError.readLine()) != null) {
				System.out.println(s);

			}

		} catch (IOException ex) {
			Logger.getLogger(CoverageCompare.class.getName()).log(Level.SEVERE, null, ex);
		}

		return output;
	}

	public static String fillDatabase(File dir, String projectName, String outputPathName) throws ClassNotFoundException, SQLException {

		String _database = outputPathName + File.separator + "gitdataminer_" + projectName + ".sqlite";

		DominoesSQLDao.openDatabase(_database); //create database


		RepositoryNode _repo = new RepositoryNode(dir, "");

		DominoesSQLDao.addRepository(_repo); //add commits

		_repo.update(); //atualiza os commitNode

		for (int i=0; i < _repo.getTotalCommits(); i++) {
			CommitNode commit = _repo.getNextCommit();
			commit.parse(_repo);
			DominoesSQLDao.addCommit(commit, _repo); 
		}
		return _database;
	}

	public static List<ClassLanguageContructs> extractAST(String projectPath) {

		ProjectAST projectAST = new ProjectAST(projectPath);

		return projectAST.getClassesLanguageConstructs();
	}

	public static List<ClassLanguageContructs> generateASTFiles(List<ClassLanguageContructs> ASTProject, List<String> changedFiles) {
		List<ClassLanguageContructs> ASTchangedFiles = new ArrayList<ClassLanguageContructs>();

		//VRF se os caminhos dos arquivos da AST contém os nomes dos arquivos modificados
		for (String file : changedFiles) {
			for (ClassLanguageContructs AST : ASTProject) {
				if (containsPath(AST.getPath(), file)) {
					ASTchangedFiles.add(AST);
				}
			}
		}
		return ASTchangedFiles;
	}

	public static boolean containsPath(String path, String relativePath) {
		String pathClean = cleanPath(path);
		String relativeClean = cleanPath(relativePath);

		return pathClean.contains(relativeClean);
	}

	public static String cleanPath(String path) {

		while (path.contains("/")) {
			path = path.replace("/", "");
		}

		while (path.contains("\\")) {
			path = path.replace("\\", "");
		}

		return path;
	}
	public static List<EditedMethod> generateEditedMethod(List<ChunkInformation> cis, String projectPath, List<ClassLanguageContructs> AST, List<ClassLanguageContructs> ASTmergeBase) throws IOException {

		Set<EditedMethod> methods = new HashSet<EditedMethod>();

		for (ChunkInformation ci : cis) {

			String ciFilePath = ci.getFilePath();
			String className = ciFilePath.substring(ciFilePath.lastIndexOf("/") + 1, ciFilePath.lastIndexOf("."));

			//Find method declaration that has some intersection with a method declaration
			List<MyMethodDeclaration> MethodDeclarations = leftCCMethodDeclarations(projectPath, ci, AST); 
			List<MyMethodDeclaration> MethodDeclarationsBase = leftBaseCCMethodDeclarations(projectPath, ci, ASTmergeBase);

			//Union Left and Base methodDeclarations 
			for (MyMethodDeclaration MethodDeclarationBase : MethodDeclarationsBase) {
				MethodDeclarations.add(MethodDeclarationBase);
			}



			if (MethodDeclarations.size() > 1) {

				//del equals method   
				for (int i = MethodDeclarations.size() - 1; i > 0; i--) {
					IMethodBinding methodDeclaration1 = MethodDeclarations.get(i).getMethodDeclaration().resolveBinding();
					IMethodBinding methodDeclaration2 = MethodDeclarations.get(i - 1).getMethodDeclaration().resolveBinding();

					if (methodDeclaration1 != null && methodDeclaration2 != null && methodDeclaration1.isEqualTo(methodDeclaration2)) {
						MethodDeclarations.remove(MethodDeclarations.get(i));
					}
				}
			}
			/*
			//del equals method   
			for (int i = MethodDeclarations.size() - 1; i > 0; i--) {
				if (MethodDeclarations.get(i).equals(MethodDeclarations.get(i - 1))) {
					MethodDeclarations.remove(MethodDeclarations.get(i));
				}
			}*/

			for(MyMethodDeclaration methodDeclaration : MethodDeclarations) {
				methods.add(new EditedMethod(className + "$" + methodDeclaration.getMethodDeclaration().getName()));

			}
		}
		return new ArrayList<EditedMethod>(methods);
	}

	public static List<MyMethodDeclaration> leftCCMethodDeclarations(String projectPath,
			ChunkInformation ci, List<ClassLanguageContructs> ASTLeft) {

		List<MyMethodDeclaration> result = new ArrayList<>();
		List<Integer> repositionBase = new ArrayList<>();
		List<Operation> operations = new ArrayList<>();

		String relativePath = null;

		if (ci.isRenamed() && ci.getRelativePathRight() != null) {
			relativePath = ci.getRelativePathRight();
		} else {
			relativePath = ci.getFilePath().replace(projectPath, "");
		}

		for (ClassLanguageContructs AST : ASTLeft) {

			if (containsPath(AST.getPath(), relativePath)) {

				List<MyMethodDeclaration> methodDeclarations = AST.getMethodDeclarations();

				for (MyMethodDeclaration methodDeclaration : methodDeclarations) {
					operations = ci.getOperations();
					for (Operation operation : operations) {
						int line = operation.getLine();
						if (leftHasIntersection(methodDeclaration, ci, line)) {
							result.add(methodDeclaration);
						}
					}
				}
			}
		}

		return result;
	}

	public static List<MyMethodDeclaration> leftBaseCCMethodDeclarations(String projectPath,
            ChunkInformation ci, List<ClassLanguageContructs> ASTmergeBase) {

        List<MyMethodDeclaration> result = new ArrayList<>();
        List<Operation> operationsBase = new ArrayList<>();

        String relativePath = null;

        if (ci.isRenamed() && ci.getRelativePathLeftBase() != null) {
            relativePath = ci.getRelativePathLeftBase();
        } else {
            relativePath = ci.getFilePath().replace(projectPath, "");
        }

        for (ClassLanguageContructs AST : ASTmergeBase) {

            if (containsPath(AST.getPath(), relativePath)) {

                List<MyMethodDeclaration> methodDeclarations = AST.getMethodDeclarations();

                for (MyMethodDeclaration methodDeclaration : methodDeclarations) {
                    operationsBase = ci.getOperationsBase();
                    for (Operation operation : operationsBase) {
                        int line = operation.getLine();
                        if (leftHasIntersection(methodDeclaration, ci, line)) {
                            result.add(methodDeclaration);
                        }
                    }
                }
            }
        }

        return result;
    }

	public static boolean leftHasIntersection(MyMethodDeclaration methodDeclaration, ChunkInformation ci, int line) {

		return leftHasIntersection(methodDeclaration.getLocation(), ci, line);

	}

	public static boolean leftHasIntersection(Location location, ChunkInformation ci, int line) {

		if ((line >= location.getElementLineBegin()) && (line <= location.getElementLineEnd())) {
			return true;
		} else {
			return false;
		}
	}

}
