package ai.stainless

import ai.stainless.Branch
import ai.stainless.Semver
import ai.stainless.jenkins.ReleaseManager
import ai.stainless.jenkins.Tags

tags = Tags.parse('''
''')
assert tags.size() == 0

// Test parse
tags29 = Tags.parse('''refs/tags/beakerx-notebook@0.9.0=9890f0b
refs/tags/myproject_1@0.9.0=16227b7
refs/tags/myproject_3@0.1.0=aff5e6b
refs/tags/myproject_3@1.0.0=aff5e6b
refs/tags/myproject_3@1.1.0=880f860
refs/tags/myproject_3@1.2.0=f1e78a7
refs/tags/myproject_7@0.0.1=e7ee5d9
refs/tags/myproject-5@0.0.1=9b23988
refs/tags/myproject_7@0.1.0=6f507c5
refs/tags/myproject_3@2.0.0=999cf2d
refs/tags/myproject-5@1.0.0=999cf2d
refs/tags/myproject_2@0.5.0=e81efa1
refs/tags/myproject_7@0.5.0=16dc4a5
refs/tags/myproject_7@0.6.0=72bb63f
refs/tags/myproject_2@0.6.0=a7313b4
refs/tags/myproject_1-v0.10.0=3a94f1b
refs/tags/myproject-6@0.6.0=cd8f353
refs/tags/myproject_3@2.1.0=cd8f353
refs/tags/myproject_2@0.6.1=d508586
refs/tags/myproject_3@2.1.1=d508586
refs/tags/myproject_1@0.10.1=d508586
refs/tags/myproject-4@0.9.0=b78f86e
refs/tags/myproject-4@1.0.0=3e5953a
refs/tags/myproject_2@0.7.0=7472271
refs/tags/myproject_1@0.11.0=da22487
refs/tags/myproject_3@2.2.0=da22487
refs/tags/myproject_1@1.0.0=a4ecaf5
refs/tags/myproject_3@2.3.0=a4ecaf5
refs/tags/myproject_2@1.0.0=a4ecaf5
''')
assert tags29.size() == 29

tags29semvers = tags29.toSemverList()

assert tags29semvers.size() == 29
assert tags29semvers.last().versionString() == '1.0.0'
assert tags29semvers.last().artifactName() == 'myproject_2-1.0.0'
assert tags29.sortedByVersion()?.last()?.versionString() == '2.3.0'
assert tags29.sortedByVersion('myproject_2')?.last()?.versionString() == '1.0.0'
assert tags29.sortedByVersion('myproject_3')?.last()?.versionString() == '2.3.0'
assert tags29.sortedByVersion('myproject_1')?.last()?.versionString() == '1.0.0'



