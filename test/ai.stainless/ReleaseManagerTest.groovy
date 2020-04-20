import ai.stainless.jenkins.ReleaseManager

class TestScript {
    public TestScript(Map env) {
        this.env = env
    }
    def env = [:]
    def sh(Map params)  {
        def cmd = params.script.execute()
        def out = new StringBuffer()
        def err = new StringBuffer()
        cmd.consumeProcessOutput( out, err )
        cmd.waitFor()
        return out.toString()
    }
}

println new ReleaseManager(new TestScript(BUILD_NUMBER:"18",JOB_NAME:"jobby",BRANCH_NAME:"master")).semanticVersion()
println new ReleaseManager(new TestScript(BUILD_NUMBER:"18",JOB_NAME:"jobby",BRANCH_NAME:"master")).artifactName()
println new ReleaseManager(new TestScript(BUILD_NUMBER:"18",JOB_NAME:"jobby",BRANCH_NAME:"master")).artifactVersion()
println new ReleaseManager(new TestScript(BUILD_NUMBER:"18",JOB_NAME:"jobby",BRANCH_NAME:"develop")).artifactName()
println new ReleaseManager(new TestScript(BUILD_NUMBER:"18",JOB_NAME:"jobby",BRANCH_NAME:"v0.0.2")).artifactName()
println new ReleaseManager(new TestScript(BUILD_NUMBER:"18",JOB_NAME:"jobby",BRANCH_NAME:"v0.0.2")).artifactVersion()
println new ReleaseManager(new TestScript(BUILD_NUMBER:"18",JOB_NAME:"jobby",BRANCH_NAME:"myfeaturebranch")).artifactName()
println new ReleaseManager(new TestScript(BUILD_NUMBER:"18",JOB_NAME:"jobby",BRANCH_NAME:"myfeaturebranch")).artifactVersion()

/*
println new ReleaseManager(new TestScript(BUILD_NUMBER:"18",JOB_NAME:"jobby",BRANCH_NAME:"master")).artifactName()
println new ReleaseManager(new TestScript(BUILD_NUMBER:"256",JOB_NAME:"jobby",BRANCH_NAME:"develop")).artifactName()
println new ReleaseManager(new TestScript(BUILD_NUMBER:"512",JOB_NAME:"jobby",BRANCH_NAME:"v1.2.3")).artifactName()
println new ReleaseManager(new TestScript(BUILD_NUMBER:"1024",JOB_NAME:"jobby",BRANCH_NAME:"jobbyjob-v4.5.6")).artifactName()
println new ReleaseManager(new TestScript(BUILD_NUMBER:"1024",JOB_NAME:"jobby",BRANCH_NAME:"jobbyjob@4.5.6")).artifactName()
 */