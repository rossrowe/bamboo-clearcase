[@ui.bambooSection ]
    [@ww.select label='ClearCase Type' name='custom.repository.cc.clearcaseType'
        list="{'UCM', 'Base'}" description='ClearCase type to use.' toggle='true' /]
[/@ui.bambooSection ]

[@ui.bambooSection dependsOn='custom.repository.cc.clearcaseType' showOn='Base']
    [@ww.textfield name='custom.repository.cc.base.viewLocation' label='Snapshot View Location' required='true'
        description='Path to ClearCase snapshot view.' /]
    [@ww.textfield name='custom.repository.cc.base.vobDir' label='VOB Directory' required='true'
        description='Relative path to a sub directory of the specified view location.
                     The resulting directory must be inside a VOB.
                     This VOB will be monitored for changes recursively from this directory down.' /]
    [@ww.textfield name='custom.repository.cc.base.branch' label='Branch' required='true'
        description='ClearCase branch.' /]
    
[/@ui.bambooSection ]

[@ui.bambooSection dependsOn='custom.repository.cc.clearcaseType' showOn='UCM']
[@ui.bambooSection ]
    [@ww.textfield name='custom.repository.cc.projectName' label='Project Name' required='true'
        description='The ClearCase selector that identifies the ClearCase UCM project to which is being built.' /]
    [@ww.textfield name='custom.repository.cc.mainComponent' label='Main Component' required='true'
        description='The component in this project that contain the baseline that identifies release, normally a non-rooted component (eg project_nr@\\pvob).' /]
    [@ww.textfield name='custom.repository.cc.intStream' label='Integration Stream' required='false'
        description='(Optional) Determine from the project if not specified. 
                     This is the stream on which baselines are made to trigger new a new build. eg stream:ProjectName_Integration@\\pvob' /]
[/@ui.bambooSection ]

[@ww.checkbox label='Compare Baselines' name='custom.repository.cc.compareBaselines' toggle='true' nameValue='true'
        description='If set, then differences between baselines will be checked, and a build will execute when changes are identified.  If unchecked, then differences between the last time the build was run will be checked.'/]

[@ww.checkbox label='Automatic Create' name='custom.repository.cc.autocreate' toggle='true' nameValue='true'
        description='If set the build view and stream are automtically created. When unchecked an existing stream and view are required'/]

[@ww.checkbox label='Disable Automatic Updates' name='custom.repository.cc.disableUpdate' toggle='true' nameValue='true'
        description='If set the ClearCase view is not updated by Bamboo'/]

[@ui.bambooSection dependsOn='custom.repository.cc.autocreate' showOn='true']
    [@ww.textfield name='custom.repository.cc.buildprefix' label='Build Prefix' required='true'
        description='The prefix to used for build created streams an views (default build_ro_)' /]
    [@ww.textfield name='custom.repository.cc.storagedir' label='View Storage Directory' required='true'
description='Shared directory where view storgae is create. default will use global view.storage settting' /]
    [@ww.textfield name='custom.repository.cc.viewTag' label='View Tag' required='false'
        description='(Optional) The tag to use for created streams (defaults to the viewName if not set)' /]
[@ww.textarea name='custom.repository.cc.loadrules' label='Load Rules' rows='5' cols='20' description='Enter the load rules for the view'/]
[/@ui.bambooSection ]

[@ui.bambooSection dependsOn='custom.repository.cc.autocreate' showOn='false']
    [@ww.textfield name='custom.repository.cc.buildStream' label='Build Stream' required='true'
        description='A ClearCase selector that identifies the stream (normally read only) against which the build occurs. eg stream:build_ro_ProjectName\\pvob' /]
    [@ww.textfield name='custom.repository.cc.viewLocation' label='View Location'
        description='(Optional) Path to ClearCase view on Build Stream mentioned above.  If not specified then a view will be created' /]    

[/@ui.bambooSection ]

[/@ui.bambooSection ]

[@ww.checkbox label='Dynamic View' name='custom.repository.cc.dynamicView' toggle='true' nameValue='true'
        description='(Optional) Specifies whether the view is a dynamic view'/]