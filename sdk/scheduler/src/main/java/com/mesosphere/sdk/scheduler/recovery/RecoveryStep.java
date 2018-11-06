package com.mesosphere.sdk.scheduler.recovery;

import com.mesosphere.sdk.offer.LaunchOfferRecommendation;
import com.mesosphere.sdk.offer.OfferRecommendation;
import com.mesosphere.sdk.scheduler.plan.DeploymentStep;
import com.mesosphere.sdk.scheduler.plan.PodInstanceRequirement;
import com.mesosphere.sdk.scheduler.recovery.constrain.LaunchConstrainer;
import com.mesosphere.sdk.state.StateStore;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * An extension of {@link DeploymentStep} meant for use with {@link DefaultRecoveryPlanManager}.
 */
public class RecoveryStep extends DeploymentStep {

  private final LaunchConstrainer launchConstrainer;

  public RecoveryStep(
      String name,
      PodInstanceRequirement podInstanceRequirement,
      LaunchConstrainer launchConstrainer,
      StateStore stateStore)
  {
    this(name, podInstanceRequirement, launchConstrainer, stateStore, Optional.empty());
  }

  public RecoveryStep(
      String name,
      PodInstanceRequirement podInstanceRequirement,
      LaunchConstrainer launchConstrainer,
      StateStore stateStore,
      Optional<String> namespace)
  {
    super(name, podInstanceRequirement, stateStore, namespace);
    this.launchConstrainer = launchConstrainer;
  }

  @Override
  public void start() {
    if (podInstanceRequirement.getRecoveryType().equals(RecoveryType.PERMANENT)) {
      FailureUtils.setPermanentlyFailed(stateStore, podInstanceRequirement.getPodInstance());
    }
  }

  @Override
  public void updateOfferStatus(Collection<OfferRecommendation> recommendations) {
    super.updateOfferStatus(recommendations);
    for (OfferRecommendation recommendation : recommendations) {
      if (recommendation instanceof LaunchOfferRecommendation) {
        launchConstrainer
            .launchHappened((LaunchOfferRecommendation) recommendation, getRecoveryType());
      }
    }
  }

  public RecoveryType getRecoveryType() {
    return podInstanceRequirement.getRecoveryType();
  }

  @Override
  public String getMessage() {
    return String.format("%s RecoveryType: %s", super.getMessage(), getRecoveryType().name());
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }
}
