package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ContactFilterInput {
    private Object status;
    private Object priority;
    private java.util.UUID assignedToId;
    private String fromDate;
    private String toDate;
    private String search;
}
