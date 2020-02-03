/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.ce.task.projectanalysis.qualitymodel;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.ViewAttributes;
import org.sonar.ce.task.projectanalysis.component.ViewsComponent;
import org.sonar.ce.task.projectanalysis.component.VisitorsCrawler;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REVIEW_RATING;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REVIEW_RATING_KEY;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.server.measure.Rating.B;
import static org.sonar.server.measure.Rating.C;

public class SecurityReviewRatingVisitorForPortfoliosTest {

  private static final int PORTFOLIO_REF = 10;
  private static final int SUB_PORTFOLIO_1_REF = 11;
  private static final int SUB_PORTFOLIO_2_REF = 12;
  private static final Component PORTFOLIO = ViewsComponent.builder(Component.Type.VIEW, Integer.toString(PORTFOLIO_REF))
    .setViewAttributes(new ViewAttributes(ViewAttributes.Type.PORTFOLIO))
    .addChildren(
      ViewsComponent.builder(Component.Type.SUBVIEW, Integer.toString(SUB_PORTFOLIO_1_REF)).build(),
      ViewsComponent.builder(Component.Type.SUBVIEW, Integer.toString(SUB_PORTFOLIO_2_REF)).build())
    .build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NCLOC)
    .add(SECURITY_HOTSPOTS)
    .add(SECURITY_REVIEW_RATING);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private VisitorsCrawler underTest = new VisitorsCrawler(singletonList(new SecurityReviewRatingVisitorForPortfolios(measureRepository, metricRepository)));

  @Test
  public void compute_security_review_rating_on_portfolio() {
    treeRootHolder.setRoot(PORTFOLIO);
    measureRepository.addRawMeasure(PORTFOLIO_REF, NCLOC_KEY, newMeasureBuilder().create(2000));
    measureRepository.addRawMeasure(PORTFOLIO_REF, SECURITY_HOTSPOTS_KEY, newMeasureBuilder().create(20));
    measureRepository.addRawMeasure(SUB_PORTFOLIO_1_REF, NCLOC_KEY, newMeasureBuilder().create(1000));
    measureRepository.addRawMeasure(SUB_PORTFOLIO_1_REF, SECURITY_HOTSPOTS_KEY, newMeasureBuilder().create(5));
    measureRepository.addRawMeasure(SUB_PORTFOLIO_2_REF, NCLOC_KEY, newMeasureBuilder().create(1000));
    measureRepository.addRawMeasure(SUB_PORTFOLIO_2_REF, SECURITY_HOTSPOTS_KEY, newMeasureBuilder().create(15));

    underTest.visit(PORTFOLIO);

    assertThat(measureRepository.getAddedRawMeasure(SUB_PORTFOLIO_1_REF, SECURITY_REVIEW_RATING_KEY).get().getIntValue()).isEqualTo(B.getIndex());
    assertThat(measureRepository.getAddedRawMeasure(SUB_PORTFOLIO_2_REF, SECURITY_REVIEW_RATING_KEY).get().getIntValue()).isEqualTo(C.getIndex());
    assertThat(measureRepository.getAddedRawMeasure(PORTFOLIO_REF, SECURITY_REVIEW_RATING_KEY).get().getIntValue()).isEqualTo(B.getIndex());
  }

  @Test
  public void compute_nothing_when_no_ncloc() {
    treeRootHolder.setRoot(PORTFOLIO);
    measureRepository.addRawMeasure(PORTFOLIO_REF, SECURITY_HOTSPOTS_KEY, newMeasureBuilder().create(2));

    underTest.visit(PORTFOLIO);

    assertThat(measureRepository.getAddedRawMeasure(PORTFOLIO_REF, SECURITY_REVIEW_RATING_KEY)).isEmpty();
  }

  @Test
  public void compute_nothing_when_no_security_hotspot() {
    treeRootHolder.setRoot(PORTFOLIO);
    measureRepository.addRawMeasure(PORTFOLIO_REF, NCLOC_KEY, newMeasureBuilder().create(1000));

    underTest.visit(PORTFOLIO);

    assertThat(measureRepository.getAddedRawMeasure(PORTFOLIO_REF, SECURITY_REVIEW_RATING_KEY)).isEmpty();
  }
}