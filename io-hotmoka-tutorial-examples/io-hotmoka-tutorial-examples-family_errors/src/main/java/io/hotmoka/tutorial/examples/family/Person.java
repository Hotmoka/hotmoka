/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.tutorial.examples.family;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.StringSupport;

@Exported
public class Person extends Storage {
  private final String name;
  private final int day;
  private final int month;
  private final int year;

  // error: arrays are not allowed in storage
  public final Person[] parents = new Person[2];

  public static int toStringCounter;

  public Person(String name, int day, int month, int year,
                Person parent1, Person parent2) {

    this.name = name;
    this.day = day;
    this.month = month;
    this.year = year;
    this.parents[0] = parent1;
    this.parents[1] = parent2;
  }

  // error: @Payable without @FromContract, missing amount and is not in Contract
  public @Payable Person(String name, int day, int month, int year) {
    this(name, day, month, year, null, null);
  }

  @Override
  public String toString() {
    toStringCounter++; // error: static update (putstatic) is not allowed
    return StringSupport.concat(name, " (", day, "/", month, "/", year, ")");
  }
}