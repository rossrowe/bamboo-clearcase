[@ui.bambooSection title='ClearCase UCM Baseline Promoter' ]
    [@ww.checkbox label='Enable baseline promotion' name='custom.cc.baselinelabeller.enabled' toggle='true'  
         description='When enabled a built baseline promotion level is set on build success of failure. 
                      By default promotion level is set to BUILT on success and REJECTED on failure but it is customisable. 
                      You also have the option of recommending the baseline on build success' /]

    [@ui.bambooSection dependsOn='custom.cc.baselinelabeller.enabled' showOn='true']
        [@ww.textfield name='custom.cc.baselinelabeller.pl.success' label='Successful build Promotion Level'
         description='(Optional) The baseline promotion level for a successful build, default is BUILT.'/]
        [@ww.textfield name='custom.cc.baselinelabeller.pl.fail' label='Failed build Promotion Level'
         description='(Optional) The baseline promotion level for a failed build, default is REJECTED.'/]
	    [@ww.checkbox label='Recommend baseline' name='custom.cc.baselinelabeller.recommend' toggle='true'  
	         description='If enabled when the build is succeesful the built baseline beceom the Integration stream recommned baseline' /]
    [/@ui.bambooSection]
[/@ui.bambooSection ]
