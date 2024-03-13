import json
import os


comparison_benchmark = ["esapi-java-legacy", "pybbs", "alfresco-community-core", "alfresco-community-remote-api", "cxf"]

human_annotated_benchmark = {
    "esapi-java-legacy": ["/Users/kknock/RIPLE/taint/summer/annotated-code/esapi-java-legacy","annotator-manual"],
    "pybbs": ["/Users/kknock/RIPLE/taint/summer/pybbs", "annotator-manual"],
    "alfresco-community-core": ["/Users/kknock/RIPLE/taint/summer/alfresco-community-repo", "annotator-manual-core",],
    "alfresco-community-remote-api": ["/Users/kknock/RIPLE/taint/summer/alfresco-community-repo", "annotator-manual-remote-api"],
    "cxf": ["/Users/kknock/RIPLE/taint/summer/cxf", "annotator-manual"],
}

annotator_benchmark = {
    "esapi-java-legacy": ["/Users/kknock/RIPLE/taint/summer/annotated-code/esapi-java-legacy", "annotationsfeb12"],
    "pybbs": ["/Users/kknock/RIPLE/taint/summer/pybbs", "annotations-feb12"],
    "alfresco-community-core": ["/Users/kknock/RIPLE/taint/summer/alfresco-community-repo", "annotations-feb12-core"],
    "alfresco-community-remote-api": ["/Users/kknock/RIPLE/taint/summer/alfresco-community-repo", "annotations-feb27-remote-api"],
    "commons-configuration": ["/Users/kknock/RIPLE/taint/summer/commons-configuration", "annotationsfeb12"],
    "cxf": ["/Users/kknock/RIPLE/taint/summer/cxf", "annotations-feb12"],
    "struts-core": ["/Users/kknock/RIPLE/taint/summer/struts", "annotations-feb12-core"],
}

def get_project_data(project):
    os.chdir(os.path.abspath(os.path.dirname(__file__)))
    human_errors = 0
    human_loc = 0
    human_annos = 0
    human_annos_per_100 = 0
    annotator_errors = 0
    annotator_loc = 0
    annotator_annos = 0
    annotator_annos_per_100 = 0
    # read from runinfo
    human_errors = find_project_branch("run_info.json", project, human_annotated_benchmark[project][1])['error_count']
    annotator_errors = find_project_branch("run_info.json", project, annotator_benchmark[project][1])['error_count']
    # read from annotation-comparison/project/branch_name.json
    os.chdir("annotation-comparison")
    os.chdir(project)
    humanjson = readJsonObj(human_annotated_benchmark[project][1]+".json")
    annotatorjson = readJsonObj(annotator_benchmark[project][1]+".json")
    human_loc = humanjson['loc']
    human_annos = humanjson['annos_total_count']
    human_annos_per_100 = humanjson['annotations_per_100_lines']
    annotator_loc = annotatorjson['loc']
    annotator_annos = annotatorjson['annos_total_count']
    annotator_annos_per_100 = annotatorjson['annotations_per_100_lines']
    
    return human_errors, human_loc, human_annos, human_annos_per_100, annotator_errors, annotator_loc, annotator_annos, annotator_annos_per_100 
    
def find_project_branch(filename, project, branch):
    with open(filename, 'r') as file:
        data = json.load(file)
    for obj in data:
        if obj['project'] == project and obj['branch'] == branch:
            return obj
    return None

def readJsonObj(filename):
    with open(filename, 'r') as file:
        data = json.load(file)
    return data
latex_table_rows = ""
for project in comparison_benchmark:
    human_errors, human_loc, human_annos, human_annos_per_100, annotator_errors, annotator_loc, annotator_annos, annotator_annos_per_100 = get_project_data(project)
    latex_table_rows += f"{project} & {human_errors} & {human_loc} & {human_annos} & {human_annos_per_100} & {annotator_errors} & {annotator_loc} & {annotator_annos} & {annotator_annos_per_100} \\\\\n"
print(latex_table_rows)
